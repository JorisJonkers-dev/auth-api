package com.jorisjonkers.personalstack.auth.application.command

import com.jorisjonkers.personalstack.auth.domain.exception.InvalidConfirmationTokenException
import com.jorisjonkers.personalstack.auth.domain.model.EmailConfirmationToken
import com.jorisjonkers.personalstack.auth.domain.port.EmailConfirmationTokenRepository
import com.jorisjonkers.personalstack.auth.domain.port.UserRepository
import com.jorisjonkers.personalstack.common.command.CommandHandler
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ConfirmEmailCommandHandler(
    private val emailConfirmationTokenRepository: EmailConfirmationTokenRepository,
    private val userRepository: UserRepository,
) : CommandHandler<ConfirmEmailCommand> {
    override fun handle(command: ConfirmEmailCommand) {
        val token = resolveValidToken(command.token)
        val user =
            userRepository.findById(token.userId)
                ?: throw InvalidConfirmationTokenException("User not found for confirmation token")
        userRepository.update(user.copy(emailConfirmed = true, updatedAt = Instant.now()))
        emailConfirmationTokenRepository.save(token.copy(usedAt = Instant.now()))
    }

    private fun resolveValidToken(rawToken: String): EmailConfirmationToken {
        val token = emailConfirmationTokenRepository.findByToken(rawToken)
        val invalidReason =
            when {
                token == null -> "Confirmation token not found"
                token.isUsed() -> "Confirmation token has already been used"
                token.isExpired() -> "Confirmation token has expired"
                else -> null
            }
        invalidReason?.let { throw InvalidConfirmationTokenException(it) }
        return checkNotNull(token) { "token was validated non-null above" }
    }
}
