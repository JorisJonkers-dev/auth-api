package com.jorisjonkers.personalstack.auth.application.command

import com.jorisjonkers.personalstack.auth.domain.event.EmailConfirmationRequestedEvent
import com.jorisjonkers.personalstack.auth.domain.event.UserRegisteredEvent
import com.jorisjonkers.personalstack.auth.domain.exception.DuplicateEmailException
import com.jorisjonkers.personalstack.auth.domain.exception.DuplicateUsernameException
import com.jorisjonkers.personalstack.auth.domain.model.EmailConfirmationToken
import com.jorisjonkers.personalstack.auth.domain.model.Role
import com.jorisjonkers.personalstack.auth.domain.model.User
import com.jorisjonkers.personalstack.auth.domain.model.UserId
import com.jorisjonkers.personalstack.auth.domain.port.EmailConfirmationTokenRepository
import com.jorisjonkers.personalstack.auth.domain.port.PasswordEncoder
import com.jorisjonkers.personalstack.auth.domain.port.UserRepository
import com.jorisjonkers.personalstack.common.command.CommandHandler
import com.jorisjonkers.personalstack.common.messaging.RabbitMqEventPublisher
import com.jorisjonkers.personalstack.common.messaging.RabbitMqMessagingProperties
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class RegisterUserCommandHandler(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val eventPublisher: ApplicationEventPublisher,
    private val rabbitMqEventPublisher: RabbitMqEventPublisher,
    private val rabbitMqMessagingProperties: RabbitMqMessagingProperties,
    private val emailConfirmationTokenRepository: EmailConfirmationTokenRepository,
) : CommandHandler<RegisterUserCommand> {
    override fun handle(command: RegisterUserCommand) {
        checkNoDuplicates(command)
        val savedUser = createUser(command)
        val confirmationToken = createAndSaveConfirmationToken(savedUser)
        publishEvents(savedUser, confirmationToken)
    }

    private fun checkNoDuplicates(command: RegisterUserCommand) {
        if (userRepository.existsByUsername(command.username)) {
            throw DuplicateUsernameException(command.username)
        }
        if (userRepository.existsByEmail(command.email)) {
            throw DuplicateEmailException(command.email)
        }
    }

    private fun createUser(command: RegisterUserCommand): User {
        val now = Instant.now()
        val user =
            User(
                id = UserId(UUID.randomUUID()),
                username = command.username,
                email = command.email,
                firstName = command.firstName,
                lastName = command.lastName,
                role = Role.USER,
                emailConfirmed = false,
                totpEnabled = false,
                createdAt = now,
                updatedAt = now,
            )
        val passwordHash = passwordEncoder.encode(command.password)
        return userRepository.create(user, passwordHash)
    }

    private fun createAndSaveConfirmationToken(user: User): EmailConfirmationToken {
        val token =
            EmailConfirmationToken(
                id = UUID.randomUUID(),
                userId = user.id,
                token = UUID.randomUUID().toString(),
                expiresAt = Instant.now().plus(TOKEN_EXPIRY_HOURS, ChronoUnit.HOURS),
                usedAt = null,
                createdAt = Instant.now(),
            )
        emailConfirmationTokenRepository.save(token)
        return token
    }

    private fun publishEvents(
        user: User,
        confirmationToken: EmailConfirmationToken,
    ) {
        val event =
            UserRegisteredEvent(
                userId = user.id,
                username = user.username,
                email = user.email,
            )
        // Intra-service event (Spring Modulith)
        eventPublisher.publishEvent(event)
        // Inter-service event (RabbitMQ for other services to consume)
        rabbitMqEventPublisher.publish(userRegisteredRoutingKey(), event)

        // Email confirmation event
        eventPublisher.publishEvent(
            EmailConfirmationRequestedEvent(
                userId = user.id,
                username = user.username,
                email = user.email,
                confirmationToken = confirmationToken.token,
            ),
        )
    }

    private fun userRegisteredRoutingKey(): String =
        rabbitMqMessagingProperties.bindings[USER_REGISTERED_BINDING]?.routingKey
            ?: error("Missing extratoast.messaging binding '$USER_REGISTERED_BINDING'")

    companion object {
        private const val TOKEN_EXPIRY_HOURS = 24L
        private const val USER_REGISTERED_BINDING = "user-registered"
    }
}
