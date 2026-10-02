package com.jorisjonkers.personalstack.auth.infrastructure.security

import com.jorisjonkers.personalstack.auth.domain.port.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
import org.springframework.web.filter.OncePerRequestFilter

/** Keeps a session's roles in step with the user's current role and service permissions. */
class SessionRolesRefreshFilter(
    private val userRepository: UserRepository,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        request.getSession(false)?.let(::refresh)
        filterChain.doFilter(request, response)
    }

    private fun refresh(session: HttpSession) {
        val context = session.getAttribute(SPRING_SECURITY_CONTEXT_KEY) as? SecurityContext
        val authentication = context?.authentication as? UsernamePasswordAuthenticationToken
        val user = (authentication?.principal as? AuthenticatedUser)?.takeUnless { it.viaServiceToken }
        if (authentication == null || user == null) return

        val roles = currentRoles(user)
        if (roles != null && roles != user.roles) store(session, authentication, user, roles)
    }

    private fun currentRoles(user: AuthenticatedUser): List<String>? =
        try {
            userRepository.findById(user.userIdValue())?.let { sessionRoles(it.role, it.servicePermissions) }
        } catch (e: DataAccessException) {
            log.warn("Could not refresh session roles for user {}; keeping the stored roles", user.userId, e)
            null
        }

    private fun store(
        session: HttpSession,
        authentication: UsernamePasswordAuthenticationToken,
        user: AuthenticatedUser,
        roles: List<String>,
    ) {
        val refreshedUser = user.copy(roles = roles)
        val otherAuthorities = authentication.authorities.filterNot { it.authority in user.roles }
        val refreshed =
            UsernamePasswordAuthenticationToken(
                refreshedUser,
                authentication.credentials,
                refreshedUser.authorities + otherAuthorities,
            ).apply { details = authentication.details }

        val refreshedContext = SecurityContextHolder.createEmptyContext().apply { this.authentication = refreshed }
        session.setAttribute(SPRING_SECURITY_CONTEXT_KEY, refreshedContext)
        SecurityContextHolder.setContext(refreshedContext)
        log.info("Refreshed session roles for user {}: {} -> {}", user.userId, user.roles, roles)
    }

    private companion object {
        private val log = LoggerFactory.getLogger(SessionRolesRefreshFilter::class.java)
    }
}
