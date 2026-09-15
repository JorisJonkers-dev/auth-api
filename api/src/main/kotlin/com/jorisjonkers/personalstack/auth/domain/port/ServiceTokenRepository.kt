package com.jorisjonkers.personalstack.auth.domain.port

import com.jorisjonkers.personalstack.auth.domain.model.ServiceToken
import com.jorisjonkers.personalstack.auth.domain.model.UserId
import java.time.Instant
import java.util.UUID

interface ServiceTokenRepository {
    fun save(token: ServiceToken): ServiceToken

    fun findById(id: UUID): ServiceToken?

    fun findByTokenHash(tokenHash: String): ServiceToken?

    fun findByUserId(userId: UserId): List<ServiceToken>

    fun revoke(
        id: UUID,
        revokedAt: Instant,
    )

    fun touchLastUsedAt(
        id: UUID,
        at: Instant,
    )
}
