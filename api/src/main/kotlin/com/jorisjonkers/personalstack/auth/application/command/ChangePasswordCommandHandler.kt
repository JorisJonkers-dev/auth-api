package com.jorisjonkers.personalstack.auth.application.command

import com.jorisjonkers.personalstack.auth.domain.exception.InvalidPasswordException
import com.jorisjonkers.personalstack.auth.domain.exception.UserNotFoundException
import com.jorisjonkers.personalstack.auth.domain.model.UserCredentials
import com.jorisjonkers.personalstack.auth.domain.model.UserId
import com.jorisjonkers.personalstack.auth.domain.port.PasswordEncoder
import com.jorisjonkers.personalstack.auth.domain.port.UserRepository
import com.jorisjonkers.personalstack.common.command.CommandHandler
import org.springframework.stereotype.Service

@Service
class ChangePasswordCommandHandler(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) : CommandHandler<ChangePasswordCommand> {
    override fun handle(command: ChangePasswordCommand) {
        val credentials = resolveCredentials(command.userId)
        if (!passwordEncoder.matches(command.currentPassword, credentials.passwordHash)) {
            throw InvalidPasswordException()
        }
        userRepository.updatePassword(command.userId, passwordEncoder.encode(command.newPassword))
    }

    private fun resolveCredentials(userId: UserId): UserCredentials {
        val user = userRepository.findById(userId) ?: throw UserNotFoundException(userId)
        return userRepository.findCredentialsByUsername(user.username) ?: throw UserNotFoundException(userId)
    }
}
