package com.jorisjonkers.personalstack.auth.application.command

import com.jorisjonkers.personalstack.auth.domain.model.Role
import com.jorisjonkers.personalstack.auth.domain.model.ServicePermission
import com.jorisjonkers.personalstack.auth.domain.model.ServiceToken
import com.jorisjonkers.personalstack.auth.domain.model.User
import com.jorisjonkers.personalstack.auth.domain.model.UserId
import com.jorisjonkers.personalstack.auth.domain.port.ServiceTokenRepository
import com.jorisjonkers.personalstack.auth.domain.port.UserRepository
import com.jorisjonkers.personalstack.common.exception.NotFoundException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class MintServiceTokenCommandHandlerTest {
    private val userRepository = mockk<UserRepository>()
    private val serviceTokenRepository = mockk<ServiceTokenRepository>(relaxed = true)
    private val handler = MintServiceTokenCommandHandler(userRepository, serviceTokenRepository)

    private val userId = UserId(UUID.randomUUID())

    private fun userWithGrants(grants: Set<ServicePermission>): User =
        User(
            id = userId,
            username = "alice",
            email = "alice@example.com",
            firstName = "Alice",
            lastName = "Smith",
            role = Role.USER,
            emailConfirmed = true,
            totpEnabled = false,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            servicePermissions = grants,
        )

    @Test
    fun `handle mints a token when the user holds the grant`() {
        every { userRepository.findById(userId) } returns userWithGrants(setOf(ServicePermission.MEMORY_API))
        val tokenSlot = slot<ServiceToken>()
        every { serviceTokenRepository.save(capture(tokenSlot)) } answers { tokenSlot.captured }

        val result =
            handler.handle(
                MintServiceTokenCommand(userId, ServicePermission.MEMORY_API, "macbook-claude-code"),
            )

        assertThat(result.service).isEqualTo(ServicePermission.MEMORY_API)
        assertThat(result.label).isEqualTo("macbook-claude-code")
        assertThat(result.token).startsWith("smt_")
        assertThat(tokenSlot.captured.userId).isEqualTo(userId)
        assertThat(tokenSlot.captured.tokenHash).hasSize(64)
        assertThat(tokenSlot.captured.tokenHash).isNotEqualTo(result.token)
        verify { serviceTokenRepository.save(any()) }
    }

    @Test
    fun `handle throws MissingServiceGrantException when the user lacks the grant`() {
        every { userRepository.findById(userId) } returns userWithGrants(emptySet())

        assertThatThrownBy {
            handler.handle(MintServiceTokenCommand(userId, ServicePermission.MEMORY_MCP, "laptop"))
        }.isInstanceOf(MissingServiceGrantException::class.java)
    }

    @Test
    fun `handle throws MissingServiceGrantException when the grant is for a different service`() {
        every { userRepository.findById(userId) } returns userWithGrants(setOf(ServicePermission.MEMORY_MCP))

        assertThatThrownBy {
            handler.handle(MintServiceTokenCommand(userId, ServicePermission.MEMORY_API, "laptop"))
        }.isInstanceOf(MissingServiceGrantException::class.java)
    }

    @Test
    fun `handle throws NotFoundException when the user does not exist`() {
        every { userRepository.findById(userId) } returns null

        assertThatThrownBy {
            handler.handle(MintServiceTokenCommand(userId, ServicePermission.MEMORY_API, "laptop"))
        }.isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `handle generates a different token on every call`() {
        every { userRepository.findById(userId) } returns userWithGrants(setOf(ServicePermission.MEMORY_API))
        every { serviceTokenRepository.save(any()) } answers { firstArg() }

        val first = handler.handle(MintServiceTokenCommand(userId, ServicePermission.MEMORY_API, "a"))
        val second = handler.handle(MintServiceTokenCommand(userId, ServicePermission.MEMORY_API, "b"))

        assertThat(first.token).isNotEqualTo(second.token)
    }
}
