package com.jorisjonkers.personalstack.auth.infrastructure.web

import com.jorisjonkers.personalstack.auth.domain.model.ServicePermission
import com.jorisjonkers.personalstack.auth.infrastructure.security.AuthenticatedUser
import com.jorisjonkers.personalstack.auth.infrastructure.security.TokenService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

/**
 * Forward-auth endpoint consumed by Traefik's forwardAuth middleware.
 * Validates the session and propagates user identity via response headers so
 * downstream services can trust the caller without re-verifying the session.
 *
 * When [xForwardedHost] is present, the host is resolved to a [ServicePermission].
 * If a permission is required and the user's roles do not contain either ROLE_ADMIN
 * or the corresponding SERVICE_* claim, a 403 is returned.
 *
 * A host with no [ServicePermission] mapping is unenforced for a session -- fromHost
 * returns null and every authenticated session passes, same as today. A service-token
 * bearer caller (`user.viaServiceToken`) gets the opposite default: an unmapped host is
 * denied, because that principal carries only a single narrow SERVICE_* claim and must
 * never fall back to session-equivalent access on a route nobody scoped it for.
 */
@RestController
@RequestMapping("/api/v1/auth")
class AuthVerificationController(
    private val tokenService: TokenService,
) {
    @GetMapping("/verify")
    fun verify(
        @AuthenticationPrincipal user: AuthenticatedUser,
        request: HttpServletRequest,
        @RequestHeader(value = "X-Forwarded-Host", required = false) xForwardedHost: String?,
    ): ResponseEntity<Unit> {
        // getSession(false): a bearer-token call carries no session cookie, and must
        // not be given one -- getSession(true) would silently create and persist a
        // throwaway Valkey-backed session on every CLI request.
        request.getSession(false)?.let(::touchSession)

        val requiredPermission = ServicePermission.fromHost(xForwardedHost)
        if (!isAuthorized(user, requiredPermission)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }

        val userId = user.userId.toString()
        val rolesHeader = user.roles.joinToString(",")
        val agentsAssertion =
            tokenService.createAgentsAssertionToken(
                userId = userId,
                username = user.getUsername(),
                roles = user.roles,
            )

        return ResponseEntity
            .ok()
            .header("X-User-Id", userId)
            .header("X-User-Roles", rolesHeader)
            .header("X-Agents-Verified-Jwt", agentsAssertion)
            .build()
    }

    private fun touchSession(session: HttpSession) {
        session.setAttribute(LAST_VERIFIED_AT_SESSION_KEY, Instant.now().toEpochMilli())
    }

    // A mapped host requires ROLE_ADMIN or the matching SERVICE_* claim, for both a
    // session and a service token. An unmapped host is unenforced for a session (matches
    // today's behavior for hosts like karakeep/hermes with no ServicePermission entry),
    // but denied outright for a service token -- that principal carries only a single
    // narrow SERVICE_* claim and must never inherit a session's broader default-allow.
    private fun isAuthorized(
        user: AuthenticatedUser,
        permission: ServicePermission?,
    ): Boolean =
        if (permission != null) {
            user.roles.contains("ROLE_ADMIN") || user.roles.contains("SERVICE_${permission.name}")
        } else {
            !user.viaServiceToken
        }

    companion object {
        const val LAST_VERIFIED_AT_SESSION_KEY = "auth.lastVerifiedAt"
    }
}
