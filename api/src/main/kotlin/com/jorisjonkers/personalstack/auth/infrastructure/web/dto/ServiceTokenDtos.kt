package com.jorisjonkers.personalstack.auth.infrastructure.web.dto

import com.jorisjonkers.personalstack.auth.application.command.MintedServiceToken
import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.util.UUID

data class MintServiceTokenRequest(
    @field:NotBlank(message = "Service is required")
    val service: String,
    @field:NotBlank(message = "Label is required")
    val label: String,
)

data class MintServiceTokenResponse(
    val id: UUID,
    val token: String,
    val service: String,
    val label: String,
    val createdAt: Instant,
) {
    companion object {
        fun from(minted: MintedServiceToken): MintServiceTokenResponse =
            MintServiceTokenResponse(
                id = minted.id,
                token = minted.token,
                service = minted.service.name,
                label = minted.label,
                createdAt = minted.createdAt,
            )
    }
}
