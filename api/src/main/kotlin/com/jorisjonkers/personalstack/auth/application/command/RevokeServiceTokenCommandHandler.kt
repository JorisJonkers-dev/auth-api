package com.jorisjonkers.personalstack.auth.application.command

import com.jorisjonkers.personalstack.auth.domain.port.ServiceTokenRepository
import com.jorisjonkers.personalstack.common.command.CommandHandler
import com.jorisjonkers.personalstack.common.exception.NotFoundException
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class RevokeServiceTokenCommandHandler(
    private val serviceTokenRepository: ServiceTokenRepository,
) : CommandHandler<RevokeServiceTokenCommand> {
    override fun handle(command: RevokeServiceTokenCommand) {
        // Ownership is checked here, not delegated to the repository: a token id
        // belonging to another user reports NotFound rather than Forbidden, so a
        // caller can't use this endpoint to enumerate other users' token ids.
        val token =
            serviceTokenRepository.findById(command.tokenId)?.takeIf { it.userId == command.userId }
                ?: throw NotFoundException("ServiceToken", command.tokenId.toString())
        serviceTokenRepository.revoke(token.id, Instant.now())
    }
}
