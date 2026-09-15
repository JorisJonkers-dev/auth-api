package com.jorisjonkers.personalstack.auth.infrastructure.persistence

import com.jorisjonkers.personalstack.auth.domain.model.ServicePermission
import com.jorisjonkers.personalstack.auth.domain.model.ServiceToken
import com.jorisjonkers.personalstack.auth.domain.model.UserId
import com.jorisjonkers.personalstack.auth.domain.port.ServiceTokenRepository
import com.jorisjonkers.personalstack.auth.jooq.tables.ServiceTokens.SERVICE_TOKENS
import org.jooq.DSLContext
import org.jooq.Record
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

@Repository
class JooqServiceTokenRepository(
    private val dsl: DSLContext,
) : ServiceTokenRepository {
    private val logger = LoggerFactory.getLogger(JooqServiceTokenRepository::class.java)

    override fun save(token: ServiceToken): ServiceToken {
        dsl
            .insertInto(SERVICE_TOKENS)
            .set(SERVICE_TOKENS.ID, token.id)
            .set(SERVICE_TOKENS.USER_ID, token.userId.value)
            .set(SERVICE_TOKENS.SERVICE, token.service.name)
            .set(SERVICE_TOKENS.TOKEN_HASH, token.tokenHash)
            .set(SERVICE_TOKENS.LABEL, token.label)
            .set(SERVICE_TOKENS.CREATED_AT, token.createdAt.toLocalDateTime())
            .set(SERVICE_TOKENS.LAST_USED_AT, token.lastUsedAt?.toLocalDateTime())
            .set(SERVICE_TOKENS.REVOKED_AT, token.revokedAt?.toLocalDateTime())
            .execute()
        return token
    }

    override fun findById(id: UUID): ServiceToken? =
        dsl
            .selectFrom(SERVICE_TOKENS)
            .where(SERVICE_TOKENS.ID.eq(id))
            .fetchOne()
            ?.toDomain()

    override fun findByTokenHash(tokenHash: String): ServiceToken? =
        dsl
            .selectFrom(SERVICE_TOKENS)
            .where(SERVICE_TOKENS.TOKEN_HASH.eq(tokenHash))
            .fetchOne()
            ?.toDomain()

    override fun findByUserId(userId: UserId): List<ServiceToken> =
        dsl
            .selectFrom(SERVICE_TOKENS)
            .where(SERVICE_TOKENS.USER_ID.eq(userId.value))
            .fetch()
            .map { it.toDomain() }

    override fun revoke(
        id: UUID,
        revokedAt: Instant,
    ) {
        dsl
            .update(SERVICE_TOKENS)
            .set(SERVICE_TOKENS.REVOKED_AT, revokedAt.toLocalDateTime())
            .where(SERVICE_TOKENS.ID.eq(id))
            .execute()
    }

    override fun touchLastUsedAt(
        id: UUID,
        at: Instant,
    ) {
        dsl
            .update(SERVICE_TOKENS)
            .set(SERVICE_TOKENS.LAST_USED_AT, at.toLocalDateTime())
            .where(SERVICE_TOKENS.ID.eq(id))
            .execute()
    }

    private fun Instant.toLocalDateTime() = atOffset(ZoneOffset.UTC).toLocalDateTime()

    private fun Record.toDomain(): ServiceToken {
        val serviceName = this[SERVICE_TOKENS.SERVICE] as String
        val service =
            runCatching { ServicePermission.valueOf(serviceName) }.getOrElse {
                logger.warn("Ignoring unknown service permission '{}' on service_tokens row", serviceName)
                throw it
            }
        return ServiceToken(
            id = this[SERVICE_TOKENS.ID] as UUID,
            userId = UserId(this[SERVICE_TOKENS.USER_ID] as UUID),
            service = service,
            tokenHash = this[SERVICE_TOKENS.TOKEN_HASH] as String,
            label = this[SERVICE_TOKENS.LABEL] as String,
            createdAt =
                (this[SERVICE_TOKENS.CREATED_AT] as java.time.LocalDateTime)
                    .toInstant(ZoneOffset.UTC),
            lastUsedAt =
                this[SERVICE_TOKENS.LAST_USED_AT]
                    ?.toInstant(ZoneOffset.UTC),
            revokedAt =
                this[SERVICE_TOKENS.REVOKED_AT]
                    ?.toInstant(ZoneOffset.UTC),
        )
    }
}
