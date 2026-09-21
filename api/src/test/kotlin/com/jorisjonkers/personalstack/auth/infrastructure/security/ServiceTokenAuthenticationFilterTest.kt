package com.jorisjonkers.personalstack.auth.infrastructure.security

import com.jorisjonkers.personalstack.auth.domain.model.Role
import com.jorisjonkers.personalstack.auth.domain.model.ServicePermission
import com.jorisjonkers.personalstack.auth.domain.model.ServiceToken
import com.jorisjonkers.personalstack.auth.domain.model.User
import com.jorisjonkers.personalstack.auth.domain.model.UserId
import com.jorisjonkers.personalstack.auth.domain.port.ServiceTokenRepository
import com.jorisjonkers.personalstack.auth.domain.port.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.apache.commons.codec.digest.DigestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import java.time.Instant
import java.util.UUID

class ServiceTokenAuthenticationFilterTest {
    private val serviceTokenRepository = mockk<ServiceTokenRepository>(relaxed = true)
    private val userRepository = mockk<UserRepository>()
    private val filter = ServiceTokenAuthenticationFilter(serviceTokenRepository, userRepository)

    private val userId = UserId(UUID.randomUUID())
    private val rawToken = "smt_test-raw-token"
    private val tokenHash = DigestUtils.sha256Hex(rawToken)

    private val user =
        User(
            id = userId,
            username = "alice",
            email = "alice@example.com",
            firstName = "",
            lastName = "",
            role = Role.USER,
            emailConfirmed = true,
            totpEnabled = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
        )

    private fun serviceToken(revoked: Boolean = false) =
        ServiceToken(
            id = UUID.randomUUID(),
            userId = userId,
            service = ServicePermission.MEMORY_API,
            tokenHash = tokenHash,
            label = "laptop",
            createdAt = Instant.now(),
            lastUsedAt = null,
            revokedAt = if (revoked) Instant.now() else null,
        )

    @AfterEach
    fun clearContext() {
        SecurityContextHolder.clearContext()
    }

    private fun runFilter(request: MockHttpServletRequest) {
        val response = MockHttpServletResponse()
        var chainCalled = false
        filter.doFilter(request, response) { _, _ -> chainCalled = true }
        assertThat(chainCalled).isTrue()
    }

    @Test
    fun `authenticates with a single SERVICE claim for a valid unrevoked token`() {
        every { serviceTokenRepository.findByTokenHash(tokenHash) } returns serviceToken()
        every { userRepository.findById(userId) } returns user
        val request = MockHttpServletRequest().apply { addHeader("Authorization", "Bearer $rawToken") }

        runFilter(request)

        val authentication = SecurityContextHolder.getContext().authentication
        assertThat(authentication).isNotNull
        val principal = authentication!!.principal as AuthenticatedUser
        assertThat(principal.roles).containsExactly("SERVICE_MEMORY_API")
        assertThat(principal.userId).isEqualTo(userId.value)
        assertThat(principal.viaServiceToken).isTrue()
        verify { serviceTokenRepository.touchLastUsedAt(any(), any()) }
    }

    @Test
    fun `does not authenticate a revoked token`() {
        every { serviceTokenRepository.findByTokenHash(tokenHash) } returns serviceToken(revoked = true)
        val request = MockHttpServletRequest().apply { addHeader("Authorization", "Bearer $rawToken") }

        runFilter(request)

        assertThat(SecurityContextHolder.getContext().authentication).isNull()
        verify(exactly = 0) { serviceTokenRepository.touchLastUsedAt(any(), any()) }
    }

    @Test
    fun `does not authenticate an unknown token`() {
        every { serviceTokenRepository.findByTokenHash(any()) } returns null
        val request = MockHttpServletRequest().apply { addHeader("Authorization", "Bearer $rawToken") }

        runFilter(request)

        assertThat(SecurityContextHolder.getContext().authentication).isNull()
    }

    @Test
    fun `does nothing without an Authorization header`() {
        val request = MockHttpServletRequest()

        runFilter(request)

        assertThat(SecurityContextHolder.getContext().authentication).isNull()
        verify(exactly = 0) { serviceTokenRepository.findByTokenHash(any()) }
    }

    @Test
    fun `does nothing for a non-Bearer Authorization header`() {
        val request = MockHttpServletRequest().apply { addHeader("Authorization", "Basic dXNlcjpwYXNz") }

        runFilter(request)

        assertThat(SecurityContextHolder.getContext().authentication).isNull()
        verify(exactly = 0) { serviceTokenRepository.findByTokenHash(any()) }
    }
}
