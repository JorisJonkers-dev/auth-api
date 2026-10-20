package com.jorisjonkers.personalstack.auth.persistence

import com.jorisjonkers.personalstack.auth.IntegrationTestBase
import com.jorisjonkers.personalstack.auth.domain.model.Role
import com.jorisjonkers.personalstack.auth.domain.model.ServicePermission
import com.jorisjonkers.personalstack.auth.domain.model.User
import com.jorisjonkers.personalstack.auth.domain.model.UserId
import com.jorisjonkers.personalstack.auth.domain.port.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import java.time.Instant
import java.util.UUID

/** V11 removes every KNOWLEDGE_API grant and token, and nothing else. */
class DropKnowledgeApiMigrationIntegrationTest : IntegrationTestBase() {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var dsl: DSLContext

    @Test
    fun `V11 drops knowledge-api grants and tokens and keeps every other row`() {
        val user = user("kb-grantee")
        userRepository.create(user, "\$2a\$10\$hash")
        grant(user, "KNOWLEDGE_API")
        grant(user, ServicePermission.VAULT.name)
        token(user, "KNOWLEDGE_API")
        token(user, ServicePermission.MEMORY_API.name)

        dsl.execute(ClassPathResource(MIGRATION).getContentAsString(Charsets.UTF_8))

        assertThat(services("user_service_permissions", user)).containsExactly(ServicePermission.VAULT.name)
        assertThat(services("service_tokens", user)).containsExactly(ServicePermission.MEMORY_API.name)
    }

    private fun grant(
        user: User,
        service: String,
    ) {
        dsl.execute("INSERT INTO user_service_permissions (user_id, service) VALUES (?, ?)", user.id.value, service)
    }

    private fun token(
        user: User,
        service: String,
    ) {
        dsl.execute(
            "INSERT INTO service_tokens (id, user_id, service, token_hash, label) VALUES (?, ?, ?, ?, ?)",
            UUID.randomUUID(),
            user.id.value,
            service,
            UUID.randomUUID().toString(),
            "cli",
        )
    }

    private fun services(
        table: String,
        user: User,
    ): List<String> =
        dsl
            .fetch("SELECT service FROM $table WHERE user_id = ?", user.id.value)
            .map { it.get(0, String::class.java) }

    private fun user(username: String): User {
        val now = Instant.now()
        return User(
            id = UserId(UUID.randomUUID()),
            username = username,
            email = "$username@example.com",
            firstName = "",
            lastName = "",
            role = Role.USER,
            emailConfirmed = true,
            totpEnabled = false,
            createdAt = now,
            updatedAt = now,
        )
    }

    private companion object {
        const val MIGRATION = "db/migration/V11__drop_knowledge_api_permission.sql"
    }
}
