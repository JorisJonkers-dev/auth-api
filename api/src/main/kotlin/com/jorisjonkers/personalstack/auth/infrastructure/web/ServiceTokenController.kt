package com.jorisjonkers.personalstack.auth.infrastructure.web

import com.jorisjonkers.personalstack.auth.application.command.MintServiceTokenCommand
import com.jorisjonkers.personalstack.auth.application.command.MintServiceTokenCommandHandler
import com.jorisjonkers.personalstack.auth.application.command.RevokeServiceTokenCommand
import com.jorisjonkers.personalstack.auth.domain.model.ServicePermission
import com.jorisjonkers.personalstack.auth.infrastructure.security.AuthenticatedUser
import com.jorisjonkers.personalstack.auth.infrastructure.web.dto.MintServiceTokenRequest
import com.jorisjonkers.personalstack.auth.infrastructure.web.dto.MintServiceTokenResponse
import com.jorisjonkers.personalstack.common.command.CommandBus
import com.jorisjonkers.personalstack.common.exception.DomainException
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Self-service, per-host bearer credentials for headless CLI/agent access through
 * forward-auth. Session-authenticated only: minting requires the caller already
 * hold the target ServicePermission grant, so a token is a scoped, revocable
 * derivative of a grant the estate already models, never a new one.
 */
@RestController
@RequestMapping("/api/v1/auth/service-tokens")
class ServiceTokenController(
    private val mintServiceTokenCommandHandler: MintServiceTokenCommandHandler,
    private val commandBus: CommandBus,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun mint(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @Valid @RequestBody request: MintServiceTokenRequest,
    ): MintServiceTokenResponse {
        validateRequestBody(request)
        val service = parseService(request.service)
        val minted =
            mintServiceTokenCommandHandler.handle(
                MintServiceTokenCommand(
                    userId = user.userIdValue(),
                    service = service,
                    label = request.label,
                ),
            )
        return MintServiceTokenResponse.from(minted)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun revoke(
        @AuthenticationPrincipal user: AuthenticatedUser,
        @PathVariable id: UUID,
    ) {
        commandBus.dispatch(RevokeServiceTokenCommand(userId = user.userIdValue(), tokenId = id))
    }

    private fun parseService(service: String): ServicePermission =
        runCatching { ServicePermission.valueOf(service.uppercase()) }.getOrElse {
            throw InvalidServicePermissionException(service)
        }
}

class InvalidServicePermissionException(
    service: String,
) : DomainException("Invalid service: $service", "INVALID_SERVICE")
