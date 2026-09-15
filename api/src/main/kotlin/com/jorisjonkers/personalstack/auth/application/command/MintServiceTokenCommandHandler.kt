package com.jorisjonkers.personalstack.auth.application.command

import com.jorisjonkers.personalstack.auth.domain.model.ServicePermission
import com.jorisjonkers.personalstack.auth.domain.model.ServiceToken
import com.jorisjonkers.personalstack.auth.domain.port.ServiceTokenRepository
import com.jorisjonkers.personalstack.auth.domain.port.UserRepository
import com.jorisjonkers.personalstack.common.command.CommandHandlerWithResult
import com.jorisjonkers.personalstack.common.exception.DomainException
import com.jorisjonkers.personalstack.common.exception.NotFoundException
import org.apache.commons.codec.digest.DigestUtils
import org.springframework.stereotype.Service
import java.security.SecureRandom
import java.time.Instant
import java.util.Base64
import java.util.UUID

// Raw token is transient: it exists only here and on the caller's device, never
// persisted. ServiceTokenRepository stores tokenHash only.
data class MintedServiceToken(
    val id: UUID,
    val token: String,
    val service: ServicePermission,
    val label: String,
    val createdAt: Instant,
)

@Service
class MintServiceTokenCommandHandler(
    private val userRepository: UserRepository,
    private val serviceTokenRepository: ServiceTokenRepository,
) : CommandHandlerWithResult<MintServiceTokenCommand, MintedServiceToken> {
    override fun handle(command: MintServiceTokenCommand): MintedServiceToken {
        val user =
            userRepository.findById(command.userId)
                ?: throw NotFoundException("User", command.userId.value.toString())
        if (!user.servicePermissions.contains(command.service)) {
            throw MissingServiceGrantException(command.service)
        }

        val rawToken = generateRawToken()
        val now = Instant.now()
        val token =
            ServiceToken(
                id = UUID.randomUUID(),
                userId = command.userId,
                service = command.service,
                tokenHash = DigestUtils.sha256Hex(rawToken),
                label = command.label,
                createdAt = now,
                lastUsedAt = null,
                revokedAt = null,
            )
        serviceTokenRepository.save(token)

        return MintedServiceToken(
            id = token.id,
            token = rawToken,
            service = token.service,
            label = token.label,
            createdAt = token.createdAt,
        )
    }

    private fun generateRawToken(): String {
        val bytes = ByteArray(TOKEN_RANDOM_BYTES)
        SecureRandom().nextBytes(bytes)
        return TOKEN_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    companion object {
        private const val TOKEN_PREFIX = "smt_"
        private const val TOKEN_RANDOM_BYTES = 32
    }
}

// Mirrors InvalidRoleException (AdminController.kt): a plain DomainException maps to
// 400 via the shared GlobalExceptionHandler, which is correct here too -- minting a
// token for a grant you don't hold is a malformed request, not a 401/403 on /verify.
class MissingServiceGrantException(
    service: ServicePermission,
) : DomainException(
        "User does not hold the ${service.name} service grant",
        "SERVICE_GRANT_REQUIRED",
    )
