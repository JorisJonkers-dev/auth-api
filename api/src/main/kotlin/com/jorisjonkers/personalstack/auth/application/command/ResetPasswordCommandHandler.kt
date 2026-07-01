package com.jorisjonkers.personalstack.auth.application.command

import com.jorisjonkers.personalstack.auth.domain.exception.InvalidResetTokenException
import com.jorisjonkers.personalstack.auth.domain.model.PasswordResetToken
import com.jorisjonkers.personalstack.auth.domain.port.PasswordEncoder
import com.jorisjonkers.personalstack.auth.domain.port.PasswordResetTokenRepository
import com.jorisjonkers.personalstack.auth.domain.port.UserRepository
import com.jorisjonkers.personalstack.common.command.CommandHandler
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ResetPasswordCommandHandler(
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) : CommandHandler<ResetPasswordCommand> {
    override fun handle(command: ResetPasswordCommand) {
        val token = resolveValidToken(command.token)
        userRepository.updatePassword(token.userId, passwordEncoder.encode(command.newPassword))
        passwordResetTokenRepository.save(token.copy(usedAt = Instant.now()))
    }

    private fun resolveValidToken(rawToken: String): PasswordResetToken {
        val token = passwordResetTokenRepository.findByToken(rawToken)
        val invalidReason =
            when {
                token == null -> "Password reset token not found"
                token.isUsed() -> "Password reset token has already been used"
                token.isExpired() -> "Password reset token has expired"
                else -> null
            }
        invalidReason?.let { throw InvalidResetTokenException(it) }
        return checkNotNull(token) { "token was validated non-null above" }
    }
}
