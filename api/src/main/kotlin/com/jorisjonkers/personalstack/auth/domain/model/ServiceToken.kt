package com.jorisjonkers.personalstack.auth.domain.model

import java.time.Instant
import java.util.UUID

// A headless, per-user, per-host bearer credential: a scoped, revocable derivative
// of a ServicePermission grant the user already holds. Only tokenHash is persisted;
// the raw value exists once, in the mint response.
data class ServiceToken(
    val id: UUID,
    val userId: UserId,
    val service: ServicePermission,
    val tokenHash: String,
    val label: String,
    val createdAt: Instant,
    val lastUsedAt: Instant?,
    val revokedAt: Instant?,
) {
    fun isRevoked(): Boolean = revokedAt != null
}
