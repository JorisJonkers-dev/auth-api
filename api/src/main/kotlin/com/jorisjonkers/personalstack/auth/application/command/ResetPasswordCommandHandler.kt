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
        val token =
            passwordResetTokenRepository.findByToken(rawToken)
                ?: throw InvalidResetTokenException("Password reset token not found")
        if (token.isUsed()) throw InvalidResetTokenException("Password reset token has already been used")
        if (token.isExpired()) throw InvalidResetTokenException("Password reset token has expired")
        return token
    }
}
