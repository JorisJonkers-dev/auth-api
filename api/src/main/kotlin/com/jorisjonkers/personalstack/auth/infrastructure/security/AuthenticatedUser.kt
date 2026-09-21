package com.jorisjonkers.personalstack.auth.infrastructure.security

import com.jorisjonkers.personalstack.auth.domain.model.UserId
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.UUID

data class AuthenticatedUser(
    val userId: UUID,
    private val username: String,
    val roles: List<String>,
    private val passwordHash: String = "",
    // Set only by ServiceTokenAuthenticationFilter. Lets verify() require an explicit
    // ServicePermission mapping for a bearer token even on a host where a full session
    // still defaults to allow.
    val viaServiceToken: Boolean = false,
) : UserDetails {
    fun userIdValue(): UserId = UserId(userId)

    override fun getUsername(): String = username

    override fun getPassword(): String = passwordHash

    override fun getAuthorities(): Collection<GrantedAuthority> = roles.map { SimpleGrantedAuthority(it) }

    companion object {
        // Live sessions in Valkey hold this class under JDK serialization, so the
        // stream id is a released contract: adding a field without pinning it
        // makes every existing session unreadable. Never change it.
        private const val serialVersionUID: Long = -7778631777683703979L

        fun of(
            userId: UserId,
            username: String,
            roles: List<String>,
            passwordHash: String = "",
            viaServiceToken: Boolean = false,
        ): AuthenticatedUser = AuthenticatedUser(userId.value, username, roles, passwordHash, viaServiceToken)
    }
}
