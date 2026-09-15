package com.jorisjonkers.personalstack.auth.infrastructure.security

import com.jorisjonkers.personalstack.auth.domain.port.ServiceTokenRepository
import com.jorisjonkers.personalstack.auth.domain.port.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Instant

// Runs only on forwardAuthSecurityFilterChain (/api/v1/auth/verify). A present,
// valid, unrevoked Bearer token authenticates the request as the token's single
// SERVICE_<permission> claim -- never the user's full role set -- so a token
// minted for one host can never pass the /verify check for another. Absent or
// invalid, this is a no-op and whatever the session-cookie path already resolved
// (including "unauthenticated") stands.
class ServiceTokenAuthenticationFilter(
    private val serviceTokenRepository: ServiceTokenRepository,
    private val userRepository: UserRepository,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val rawToken = bearerToken(request)
        if (rawToken != null) {
            authenticate(rawToken)
        }
        filterChain.doFilter(request, response)
    }

    private fun bearerToken(request: HttpServletRequest): String? =
        request
            .getHeader(HttpHeaders.AUTHORIZATION)
            ?.takeIf { it.startsWith(BEARER_PREFIX, ignoreCase = true) }
            ?.substring(BEARER_PREFIX.length)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    private fun authenticate(rawToken: String) {
        val token =
            serviceTokenRepository
                .findByTokenHash(DigestUtils.sha256Hex(rawToken))
                ?.takeIf { !it.isRevoked() }
                ?: return
        val user = userRepository.findById(token.userId) ?: return

        val principal =
            AuthenticatedUser.of(
                userId = token.userId,
                username = user.username,
                roles = listOf("SERVICE_${token.service.name}"),
            )
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
        serviceTokenRepository.touchLastUsedAt(token.id, Instant.now())
    }

    companion object {
        private const val BEARER_PREFIX = "Bearer "
    }
}
