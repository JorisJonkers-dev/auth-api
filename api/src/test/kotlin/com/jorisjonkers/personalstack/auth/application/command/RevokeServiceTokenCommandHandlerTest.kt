package com.jorisjonkers.personalstack.auth.application.command

import com.jorisjonkers.personalstack.auth.domain.model.ServicePermission
import com.jorisjonkers.personalstack.auth.domain.model.ServiceToken
import com.jorisjonkers.personalstack.auth.domain.model.UserId
import com.jorisjonkers.personalstack.auth.domain.port.ServiceTokenRepository
import com.jorisjonkers.personalstack.common.exception.NotFoundException
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class RevokeServiceTokenCommandHandlerTest {
    private val serviceTokenRepository = mockk<ServiceTokenRepository>(relaxed = true)
    private val handler = RevokeServiceTokenCommandHandler(serviceTokenRepository)

    private val ownerId = UserId(UUID.randomUUID())
    private val otherUserId = UserId(UUID.randomUUID())
    private val tokenId = UUID.randomUUID()

    private val token =
        ServiceToken(
            id = tokenId,
            userId = ownerId,
            service = ServicePermission.MEMORY_API,
            tokenHash = "hash",
            label = "laptop",
            createdAt = Instant.now(),
            lastUsedAt = null,
            revokedAt = null,
        )

    @Test
    fun `handle revokes a token owned by the caller`() {
        every { serviceTokenRepository.findById(tokenId) } returns token

        handler.handle(RevokeServiceTokenCommand(ownerId, tokenId))

        verify { serviceTokenRepository.revoke(tokenId, any()) }
    }

    @Test
    fun `handle throws NotFoundException when the token does not exist`() {
        every { serviceTokenRepository.findById(tokenId) } returns null

        assertThatThrownBy {
            handler.handle(RevokeServiceTokenCommand(ownerId, tokenId))
        }.isInstanceOf(NotFoundException::class.java)
        verify(exactly = 0) { serviceTokenRepository.revoke(any(), any()) }
    }

    @Test
    fun `handle throws NotFoundException when the token belongs to another user`() {
        every { serviceTokenRepository.findById(tokenId) } returns token

        assertThatThrownBy {
            handler.handle(RevokeServiceTokenCommand(otherUserId, tokenId))
        }.isInstanceOf(NotFoundException::class.java)
        verify(exactly = 0) { serviceTokenRepository.revoke(any(), any()) }
    }
}
