package com.jorisjonkers.personalstack.auth.persistence

import com.jorisjonkers.personalstack.auth.IntegrationTestBase
import com.jorisjonkers.personalstack.auth.config.CacheConfig.Companion.CACHE_USERS_BY_ID
import com.jorisjonkers.personalstack.auth.config.CacheConfig.Companion.CACHE_USERS_BY_USERNAME
import com.jorisjonkers.personalstack.auth.domain.model.Role
import com.jorisjonkers.personalstack.auth.domain.model.ServicePermission
import com.jorisjonkers.personalstack.auth.domain.model.User
import com.jorisjonkers.personalstack.auth.domain.model.UserId
import com.jorisjonkers.personalstack.auth.domain.port.UserRepository
import com.jorisjonkers.personalstack.auth.infrastructure.persistence.JooqUserRepository
import com.jorisjonkers.personalstack.auth.jooq.tables.AppUser.APP_USER
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.jooq.ExecuteContext
import org.jooq.ExecuteListener
import org.jooq.impl.DSL
import org.jooq.impl.DefaultExecuteListenerProvider
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cache.CacheManager
import java.time.Instant
import java.util.UUID

private const val HASH = "\$2a\$10\$hashedPassword"
private const val EVICTION_CYCLES = 200

class JooqUserRepositoryIntegrationTest : IntegrationTestBase() {
    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var dsl: DSLContext

    @Autowired
    private lateinit var cacheManager: CacheManager

    @Test
    fun `create and findByUsername returns the saved user`() {
        val user = buildUser(username = "alice", email = "alice@example.com")
        userRepository.create(user, "\$2a\$10\$hashedPassword")

        val found = userRepository.findByUsername("alice")

        assertThat(found).isNotNull
        assertThat(found!!.username).isEqualTo("alice")
        assertThat(found.email).isEqualTo("alice@example.com")
        assertThat(found.role).isEqualTo(Role.USER)
        assertThat(found.totpEnabled).isFalse()
        assertThat(found.servicePermissions).isEmpty()
    }

    @Test
    fun `findById returns the saved user`() {
        val user = buildUser(username = "bob", email = "bob@example.com")
        userRepository.create(user, "\$2a\$10\$hashedPassword")

        val found = userRepository.findById(user.id)

        assertThat(found).isNotNull
        assertThat(found!!.id).isEqualTo(user.id)
    }

    @Test
    fun `findCredentialsByUsername returns credentials with password hash`() {
        val user = buildUser(username = "carol", email = "carol@example.com")
        userRepository.create(user, "\$2a\$10\$expectedHash")

        val credentials = userRepository.findCredentialsByUsername("carol")

        assertThat(credentials).isNotNull
        assertThat(credentials!!.username).isEqualTo("carol")
        assertThat(credentials.passwordHash).isEqualTo("\$2a\$10\$expectedHash")
        assertThat(credentials.totpEnabled).isFalse()
        assertThat(credentials.servicePermissions).isEmpty()
    }

    @Test
    fun `existsByUsername returns true when user exists`() {
        val user = buildUser(username = "dave", email = "dave@example.com")
        userRepository.create(user, "\$2a\$10\$hash")

        assertThat(userRepository.existsByUsername("dave")).isTrue()
        assertThat(userRepository.existsByUsername("unknown")).isFalse()
    }

    @Test
    fun `existsByEmail returns true when email is registered`() {
        val user = buildUser(username = "eve", email = "eve@example.com")
        userRepository.create(user, "\$2a\$10\$hash")

        assertThat(userRepository.existsByEmail("eve@example.com")).isTrue()
        assertThat(userRepository.existsByEmail("nobody@example.com")).isFalse()
    }

    @Test
    fun `update persists role and totpEnabled changes`() {
        val user = buildUser(username = "frank", email = "frank@example.com")
        userRepository.create(user, "\$2a\$10\$hash")

        val updated = user.copy(totpEnabled = true, role = Role.ADMIN)
        userRepository.update(updated)

        val found = userRepository.findById(user.id)!!
        assertThat(found.totpEnabled).isTrue()
        assertThat(found.role).isEqualTo(Role.ADMIN)
    }

    @Test
    fun `saveServicePermissions stores and findById returns them`() {
        val user = buildUser(username = "grace", email = "grace@example.com")
        userRepository.create(user, "\$2a\$10\$hash")

        userRepository.saveServicePermissions(user.id, setOf(ServicePermission.VAULT, ServicePermission.GRAFANA))

        val found = userRepository.findById(user.id)!!
        assertThat(found.servicePermissions).containsExactlyInAnyOrder(
            ServicePermission.VAULT,
            ServicePermission.GRAFANA,
        )
    }

    @Test
    fun `saveServicePermissions replaces previous permissions`() {
        val user = buildUser(username = "henry", email = "henry@example.com")
        userRepository.create(user, "\$2a\$10\$hash")

        userRepository.saveServicePermissions(user.id, setOf(ServicePermission.VAULT, ServicePermission.N8N))
        userRepository.saveServicePermissions(user.id, setOf(ServicePermission.GRAFANA))

        val found = userRepository.findById(user.id)!!
        assertThat(found.servicePermissions).containsExactly(ServicePermission.GRAFANA)
    }

    @Test
    fun `saveServicePermissions with empty set clears all permissions`() {
        val user = buildUser(username = "iris", email = "iris@example.com")
        userRepository.create(user, "\$2a\$10\$hash")

        userRepository.saveServicePermissions(user.id, setOf(ServicePermission.VAULT))
        userRepository.saveServicePermissions(user.id, emptySet())

        val found = userRepository.findById(user.id)!!
        assertThat(found.servicePermissions).isEmpty()
    }

    @Test
    fun `findById issues a single SELECT (no N+1 permission load)`() {
        // Regression guard for the multiset refactor that folds the
        // permission load into the outer query. If anyone later
        // "simplifies" it back to `loadServicePermissions(userId)`
        // inside the row mapper, this test will flip from 1 to 2
        // SELECT statements and fail the build.
        val user = buildUser(username = "n-plus-one-guard", email = "n-plus-one-guard@example.com")
        userRepository.create(user, "\$2a\$10\$hash")
        userRepository.saveServicePermissions(
            user.id,
            setOf(ServicePermission.VAULT, ServicePermission.GRAFANA),
        )

        val counter = SelectCountingListener()
        val scopedRepo =
            JooqUserRepository(
                DSL.using(
                    dsl.configuration().derive(*DefaultExecuteListenerProvider.providers(counter)),
                ),
                cacheManager,
            )

        val found = scopedRepo.findById(user.id)

        assertThat(found).isNotNull
        assertThat(found!!.servicePermissions).containsExactlyInAnyOrder(
            ServicePermission.VAULT,
            ServicePermission.GRAFANA,
        )
        assertThat(counter.selectCount)
            .describedAs(
                "findById should issue exactly 1 SELECT; if this flips to 2 the " +
                    "multiset refactor has been reverted. Statements: ${counter.statements}",
            ).isEqualTo(1)
    }

    @Test
    fun `findById returns cached value without hitting the database on repeat`() {
        val user = buildUser(username = "cache-hit", email = "cache-hit@example.com")
        userRepository.create(user, "\$2a\$10\$hash")

        // First call populates the cache.
        val first = userRepository.findById(user.id)
        assertThat(first).isNotNull

        // Nuke the row directly; the cached value must still be served.
        // If the cache were bypassed, this delete would make the second
        // findById return null.
        dsl.deleteFrom(APP_USER).where(APP_USER.ID.eq(user.id.value)).execute()

        val second = userRepository.findById(user.id)

        assertThat(second)
            .describedAs("expected Valkey-cached value; got null, which means the cache was bypassed")
            .isNotNull
        assertThat(second!!.username).isEqualTo("cache-hit")

        // Clean up so the repository's evict-on-delete path below doesn't
        // interact with other tests.
        cacheManager.getCache(CACHE_USERS_BY_ID)?.evict(user.id.value)
        cacheManager.getCache(CACHE_USERS_BY_USERNAME)?.evict("cache-hit")
    }

    @Test
    fun `update evicts the users byId cache`() {
        val user = buildUser(username = "evict-me", email = "evict-me@example.com")
        userRepository.create(user, "\$2a\$10\$hash")

        val initial = userRepository.findById(user.id)!!
        assertThat(initial.role).isEqualTo(Role.USER)

        userRepository.update(user.copy(role = Role.ADMIN))

        val refreshed = userRepository.findById(user.id)!!
        assertThat(refreshed.role).isEqualTo(Role.ADMIN)
    }

    @Test
    fun `deleteById evicts every user cache`() {
        val user = buildUser(username = "wipe-me", email = "wipe-me@example.com")
        userRepository.create(user, "\$2a\$10\$hash")

        assertThat(userRepository.findById(user.id)).isNotNull
        assertThat(userRepository.findByUsername("wipe-me")).isNotNull
        assertThat(userRepository.findByEmail("wipe-me@example.com")).isNotNull
        assertThat(userRepository.findCredentialsByUsername("wipe-me")).isNotNull

        userRepository.deleteById(user.id)

        assertThat(userRepository.findById(user.id)).isNull()
        assertThat(userRepository.findByUsername("wipe-me")).isNull()
        assertThat(userRepository.findByEmail("wipe-me@example.com")).isNull()
        assertThat(userRepository.findCredentialsByUsername("wipe-me")).isNull()
    }

    @Test
    fun `saveServicePermissions evicts the users byId cache`() {
        val user = buildUser(username = "grant-me", email = "grant-me@example.com")
        userRepository.create(user, "\$2a\$10\$hash")

        val initial = userRepository.findById(user.id)!!
        assertThat(initial.servicePermissions).isEmpty()

        userRepository.saveServicePermissions(user.id, setOf(ServicePermission.VAULT))

        val refreshed = userRepository.findById(user.id)!!
        assertThat(refreshed.servicePermissions).containsExactly(ServicePermission.VAULT)
    }

    /**
     * Regression guard for #64. Spring Data Redis defaults to asynchronous
     * writes on a reactive-capable connection factory, which Lettuce is: an
     * `evict()` then returns before Valkey has applied the DEL and the very
     * next `findById` can still be served the pre-update row, which it then
     * re-caches for the full TTL. CacheConfig pins `immediateWrites()` to
     * close that window. One cycle reproduced it in roughly 1 run in 25, so
     * this asserts over a batch rather than once.
     */
    @Test
    fun `eviction is visible to the immediately following read`() {
        val cache = cacheManager.getCache(CACHE_USERS_BY_ID)!!
        val stale = mutableListOf<String>()

        repeat(EVICTION_CYCLES) { cycle ->
            val user = buildUser(username = "evict-cycle-$cycle", email = "evict-cycle-$cycle@example.com")
            userRepository.create(user, HASH)
            userRepository.findById(user.id)

            userRepository.update(user.copy(role = Role.ADMIN))
            if (cache.get(user.id.value) != null) {
                stale += "cycle $cycle: byId entry survived update()"
            }

            userRepository.deleteById(user.id)
            if (userRepository.findById(user.id) != null) {
                stale += "cycle $cycle: findById served a deleted user"
            }
        }

        assertThat(stale).isEmpty()
    }

    @Test
    fun `saveTotpSecret is reflected on the next findCredentialsByUsername`() {
        val user = buildUser(username = "totp-secret-me", email = "totp-secret-me@example.com")
        userRepository.create(user, "\$2a\$10\$hash")

        val initial = userRepository.findCredentialsByUsername("totp-secret-me")!!
        assertThat(initial.totpSecret).isNull()

        userRepository.saveTotpSecret(user.id, "JBSWY3DPEHPK3PXP")

        val refreshed = userRepository.findCredentialsByUsername("totp-secret-me")!!
        assertThat(refreshed.totpSecret).isEqualTo("JBSWY3DPEHPK3PXP")
    }

    @Test
    fun `updatePassword is reflected on the next findCredentialsByUsername`() {
        val user = buildUser(username = "rotate-me", email = "rotate-me@example.com")
        userRepository.create(user, "\$2a\$10\$old")

        val initial = userRepository.findCredentialsByUsername("rotate-me")!!
        assertThat(initial.passwordHash).isEqualTo("\$2a\$10\$old")

        userRepository.updatePassword(user.id, "\$2a\$10\$new")

        val refreshed = userRepository.findCredentialsByUsername("rotate-me")!!
        assertThat(refreshed.passwordHash).isEqualTo("\$2a\$10\$new")
    }

    private class SelectCountingListener : ExecuteListener {
        val statements: MutableList<String> = mutableListOf()

        val selectCount: Int
            get() = statements.count { it.trimStart().startsWith("select", ignoreCase = true) }

        override fun executeStart(ctx: ExecuteContext) {
            ctx.sql()?.let(statements::add)
        }
    }

    @Test
    fun `loadServicePermissions skips rows whose enum entry was removed`() {
        // Guards the class of bug where ServicePermission.ROUTER was
        // dropped from the enum (router moved to Tailscale subnet
        // routing) but an existing user_service_permissions row remained,
        // taking /me + /session-login to 500 with an
        // IllegalArgumentException for every grantee. The loader now
        // skips unknown values + logs a warn; a follow-up migration
        // cleans the row at leisure.
        val user = buildUser(username = "orphan-grant", email = "orphan-grant@example.com")
        userRepository.create(user, "\$2a\$10\$hash")
        userRepository.saveServicePermissions(user.id, setOf(ServicePermission.VAULT))

        // Inject an orphan row directly — the domain API rejects unknown
        // values by construction.
        dsl
            .insertInto(DSL.table("user_service_permissions"))
            .columns(DSL.field("user_id"), DSL.field("service"))
            .values(user.id.value, "RETIRED_SERVICE_XYZ")
            .execute()

        val found = userRepository.findById(user.id)

        assertThat(found).isNotNull
        assertThat(found!!.servicePermissions).containsExactly(ServicePermission.VAULT)
    }

    @Test
    fun `findCredentialsByUsername includes service permissions`() {
        val user = buildUser(username = "james", email = "james@example.com")
        userRepository.create(user, "\$2a\$10\$hash")
        userRepository.saveServicePermissions(user.id, setOf(ServicePermission.AGENTS))

        val credentials = userRepository.findCredentialsByUsername("james")!!
        assertThat(credentials.servicePermissions).containsExactly(ServicePermission.AGENTS)
    }

    @Test
    fun `findAll returns all users`() {
        val before = userRepository.findAll().size
        val suffix1 = UUID.randomUUID().toString().take(6)
        val suffix2 = UUID.randomUUID().toString().take(6)
        val user1 = buildUser(username = "kyle_$suffix1", email = "kyle_$suffix1@ex.com")
        val user2 = buildUser(username = "lily_$suffix2", email = "lily_$suffix2@ex.com")
        userRepository.create(user1, "\$2a\$10\$hash")
        userRepository.create(user2, "\$2a\$10\$hash")

        val all = userRepository.findAll()
        assertThat(all.size).isEqualTo(before + 2)
    }

    @Test
    fun `deleteById removes the user`() {
        val suffix = UUID.randomUUID().toString().take(6)
        val user = buildUser(username = "mike_$suffix", email = "mike_$suffix@ex.com")
        userRepository.create(user, "\$2a\$10\$hash")

        userRepository.deleteById(user.id)

        assertThat(userRepository.findById(user.id)).isNull()
    }

    @Test
    fun `findCredentialsByLoginIdentifier resolves a username in any case`() {
        userRepository.create(buildUser(username = "joris", email = "joris@example.com"), HASH)

        for (typed in listOf("joris", "Joris", "JORIS")) {
            val found = userRepository.findCredentialsByLoginIdentifier(typed)
            assertThat(found).describedAs("typed=%s", typed).isNotNull
            assertThat(found!!.username).isEqualTo("joris")
        }
    }

    @Test
    fun `findCredentialsByLoginIdentifier resolves an email address in any case`() {
        userRepository.create(buildUser(username = "joris", email = "joris@example.com"), HASH)

        for (typed in listOf("joris@example.com", "Joris@Example.com")) {
            val found = userRepository.findCredentialsByLoginIdentifier(typed)
            assertThat(found).describedAs("typed=%s", typed).isNotNull
            assertThat(found!!.username).isEqualTo("joris")
        }
    }

    @Test
    fun `findCredentialsByLoginIdentifier returns null for an unknown identifier`() {
        userRepository.create(buildUser(username = "joris", email = "joris@example.com"), HASH)

        assertThat(userRepository.findCredentialsByLoginIdentifier("nobody")).isNull()
        assertThat(userRepository.findCredentialsByLoginIdentifier("nobody@example.com")).isNull()
    }

    // Rows that predate the case-insensitive existence checks can still collide.
    // The row returned must not depend on Postgres' scan order.
    @Test
    fun `findCredentialsByLoginIdentifier prefers an exact username over a case variant`() {
        userRepository.create(buildUser(username = "Joris", email = "upper@example.com"), HASH)
        userRepository.create(buildUser(username = "joris", email = "lower@example.com"), HASH)

        val found = userRepository.findCredentialsByLoginIdentifier("joris")

        assertThat(found?.username).isEqualTo("joris")
        assertThat(userRepository.findCredentialsByLoginIdentifier("Joris")?.username).isEqualTo("Joris")
    }

    @Test
    fun `findCredentialsByLoginIdentifier prefers a username over another account's email`() {
        userRepository.create(buildUser(username = "collision", email = "someone@example.com"), HASH)
        userRepository.create(buildUser(username = "other", email = "collision"), HASH)

        val found = userRepository.findCredentialsByLoginIdentifier("collision")

        assertThat(found?.username).isEqualTo("collision")
    }

    @Test
    fun `existsByUsername and existsByEmail ignore case`() {
        userRepository.create(buildUser(username = "joris", email = "joris@example.com"), HASH)

        assertThat(userRepository.existsByUsername("JORIS")).isTrue()
        assertThat(userRepository.existsByEmail("Joris@Example.COM")).isTrue()
        assertThat(userRepository.existsByUsername("someone-else")).isFalse()
    }

    private fun buildUser(
        username: String,
        email: String,
    ): User {
        val now = Instant.now()
        return User(
            id = UserId(UUID.randomUUID()),
            username = username,
            email = email,
            firstName = "",
            lastName = "",
            role = Role.USER,
            emailConfirmed = true,
            totpEnabled = false,
            createdAt = now,
            updatedAt = now,
        )
    }
}
