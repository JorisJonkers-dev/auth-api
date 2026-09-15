package com.jorisjonkers.personalstack.auth.infrastructure.web

import com.jorisjonkers.personalstack.auth.domain.model.UserId
import com.jorisjonkers.personalstack.auth.infrastructure.security.AuthenticatedUser
import com.jorisjonkers.personalstack.auth.infrastructure.security.TokenService
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpSession
import java.util.UUID

class AuthVerificationControllerTest {
    private val agentsAssertionToken = "agents-assertion-token"
    private val tokenService =
        mockk<TokenService> {
            every { createAgentsAssertionToken(any(), any(), any()) } returns agentsAssertionToken
        }
    private val controller = AuthVerificationController(tokenService)

    private val defaultUserId = UUID.randomUUID()

    private fun buildUserWithUuid(
        id: UUID = defaultUserId,
        roles: List<String> = listOf("ROLE_USER"),
    ): AuthenticatedUser =
        AuthenticatedUser.of(
            userId = UserId(id),
            username = "testuser",
            roles = roles,
        )

    // Attaches an existing session, mimicking a browser call that already carries a
    // session cookie -- getSession(false) will find it without creating one.
    private fun requestWithSession(session: MockHttpSession = MockHttpSession()): MockHttpServletRequest =
        MockHttpServletRequest().apply { setSession(session) }

    @Test
    fun `verify returns 200 with user headers for valid session`() {
        val user = buildUserWithUuid(roles = listOf("ROLE_USER", "SERVICE_VAULT"))
        val session = MockHttpSession()
        val request = requestWithSession(session)

        val response = controller.verify(user, request, "vault.jorisjonkers.dev")

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.headers["X-User-Id"]).containsExactly(defaultUserId.toString())
        assertThat(response.headers["X-User-Roles"]).containsExactly("ROLE_USER,SERVICE_VAULT")
        assertThat(response.headers["X-Agents-Verified-Jwt"]).containsExactly(agentsAssertionToken)
        assertThat(session.getAttribute(AuthVerificationController.LAST_VERIFIED_AT_SESSION_KEY)).isNotNull()
    }

    @Test
    fun `verify returns 403 when USER lacks service permission`() {
        val user = buildUserWithUuid(roles = listOf("ROLE_USER"))
        val session = MockHttpSession()
        val request = requestWithSession(session)

        val response = controller.verify(user, request, "vault.jorisjonkers.dev")

        assertThat(response.statusCode).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(session.getAttribute(AuthVerificationController.LAST_VERIFIED_AT_SESSION_KEY)).isNotNull()
    }

    @Test
    fun `verify returns 200 for ADMIN regardless of service`() {
        val user = buildUserWithUuid(roles = listOf("ROLE_ADMIN"))

        val response = controller.verify(user, requestWithSession(), "dashboard.jorisjonkers.dev")

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `verify returns 200 when USER has dashboard service permission`() {
        val user = buildUserWithUuid(roles = listOf("ROLE_USER", "SERVICE_DASHBOARD"))

        val response = controller.verify(user, requestWithSession(), "dashboard.jorisjonkers.dev")

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `verify returns 200 when no X-Forwarded-Host present`() {
        val user = buildUserWithUuid(roles = listOf("ROLE_USER"))

        val response = controller.verify(user, requestWithSession(), null)

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `verify propagates X-User-Id from session principal`() {
        val id = UUID.randomUUID()
        val user = buildUserWithUuid(id = id)

        val response = controller.verify(user, requestWithSession(), null)

        assertThat(response.headers["X-User-Id"]).containsExactly(id.toString())
    }

    @Test
    fun `verify propagates X-User-Roles header`() {
        val user = buildUserWithUuid(roles = listOf("ROLE_USER", "SERVICE_MAIL"))

        val response = controller.verify(user, requestWithSession(), null)

        assertThat(response.headers["X-User-Roles"]).containsExactly("ROLE_USER,SERVICE_MAIL")
    }

    @Test
    fun `verify with multiple roles joins them with comma`() {
        val user = buildUserWithUuid(roles = listOf("ROLE_USER", "SERVICE_VAULT", "SERVICE_GRAFANA"))

        val response = controller.verify(user, requestWithSession(), null)

        assertThat(response.headers["X-User-Roles"])
            .containsExactly("ROLE_USER,SERVICE_VAULT,SERVICE_GRAFANA")
    }

    @Test
    fun `unknown subdomain host passes through without permission check`() {
        val user = buildUserWithUuid(roles = listOf("ROLE_USER"))

        val response = controller.verify(user, requestWithSession(), "unknown-service.jorisjonkers.dev")

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
    }

    @Test
    fun `verify with no session on the request does not create one`() {
        val user = buildUserWithUuid(roles = listOf("ROLE_USER", "SERVICE_MEMORY_API"))
        val request = MockHttpServletRequest()

        val response = controller.verify(user, request, "memory-api.jorisjonkers.dev")

        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(request.getSession(false)).isNull()
    }
}
