package com.jorisjonkers.personalstack.auth.infrastructure.security

import com.jorisjonkers.personalstack.auth.domain.model.UserId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.ObjectStreamClass
import java.util.Base64
import java.util.UUID

/**
 * [AuthenticatedUser] is stored in a live session, so its stream identity is a
 * released contract.
 *
 * `@EnableRedisHttpSession` keeps the `SecurityContext` in Valkey under JDK
 * serialization, and this class sits inside it. Without an explicit
 * `serialVersionUID` the JVM derives one from the class shape, so adding a
 * field silently changes it. Adding `viaServiceToken` did exactly that and
 * every session written before the deploy became unreadable:
 *
 *   InvalidClassException: ...AuthenticatedUser; local class incompatible:
 *   stream classdesc serialVersionUID = -7778631777683703979,
 *   local class serialVersionUID = 2469762916733932736
 *
 * The throw happens in `SessionRepositoryFilter` while Spring Security loads
 * the deferred context, which is before any controller runs — so sign-in
 * answered a bodyless 401 with nothing in the request log, and the browser
 * could not recover by logging in again because it kept presenting the same
 * poisoned `SESSION` cookie.
 *
 * The pinned value is the one already in the live store. Java tolerates a
 * field added after the fact when the id matches, so an old session
 * deserializes with `viaServiceToken = false`, which is the safe default.
 * Never change it; add fields instead.
 */
class AuthenticatedUserSerializationTest {
    @Test
    fun `serialVersionUID stays pinned to the value live sessions were written with`() {
        val streamClass = ObjectStreamClass.lookup(AuthenticatedUser::class.java)

        assertThat(streamClass).isNotNull()
        assertThat(streamClass!!.serialVersionUID).isEqualTo(PINNED_SERIAL_VERSION_UID)
    }

    @Test
    fun `survives a serialization round trip with every field intact`() {
        val original =
            AuthenticatedUser.of(
                userId = UserId(UUID.fromString("11111111-2222-3333-4444-555555555555")),
                username = "joris",
                roles = listOf("ROLE_ADMIN", "SERVICE_OVERLEAF"),
                passwordHash = "{bcrypt}hash",
                viaServiceToken = true,
            )

        val restored = roundTrip(original)

        assertThat(restored).isEqualTo(original)
        assertThat(restored.username).isEqualTo("joris")
        assertThat(restored.viaServiceToken).isTrue()
        assertThat(restored.authorities.map { it.authority })
            .containsExactly("ROLE_ADMIN", "SERVICE_OVERLEAF")
    }

    @Test
    fun `reads a session written before viaServiceToken existed`() {
        val restored =
            ObjectInputStream(
                ByteArrayInputStream(Base64.getDecoder().decode(SESSION_WRITTEN_BEFORE_SERVICE_TOKENS)),
            ).use { it.readObject() as AuthenticatedUser }

        assertThat(restored.username).isEqualTo("joris")
        assertThat(restored.userId).isEqualTo(UUID.fromString("11111111-2222-3333-4444-555555555555"))
        assertThat(restored.roles).containsExactly("ROLE_ADMIN", "SERVICE_OVERLEAF")
        // Absent from the stream, so it takes the field default -- a session
        // predating service tokens is a full session, not a token-scoped one.
        assertThat(restored.viaServiceToken).isFalse()
    }

    private fun roundTrip(user: AuthenticatedUser): AuthenticatedUser {
        val bytes =
            ByteArrayOutputStream()
                .also { sink ->
                    ObjectOutputStream(sink).use { it.writeObject(user) }
                }.toByteArray()
        return ObjectInputStream(ByteArrayInputStream(bytes)).use {
            it.readObject() as AuthenticatedUser
        }
    }

    private companion object {
        const val PINNED_SERIAL_VERSION_UID = -7778631777683703979L

        /** Written by the class as it stood in v0.8.0, before `viaServiceToken`. */
        const val SESSION_WRITTEN_BEFORE_SERVICE_TOKENS =
            "rO0ABXNyAE1jb20uam9yaXNqb25rZXJzLnBlcnNvbmFsc3RhY2suYXV0aC5pbmZyYXN0cnVjdHVyZS5zZWN1cml0eS5BdXRoZW50aWNhdGVkVXNlcpQMv5x0mPdVAgAETAAMcGFzc3dvcmRIYXNodAASTGphdmEvbGFuZy9TdHJpbmc7TAAFcm9sZXN0ABBMamF2YS91dGlsL0xpc3Q7TAAGdXNlcklkdAAQTGphdmEvdXRpbC9VVUlEO0wACHVzZXJuYW1lcQB+AAF4cHQADHtiY3J5cHR9aGFzaHNyABNqYXZhLnV0aWwuQXJyYXlMaXN0eIHSHZnHYZ0DAAFJAARzaXpleHAAAAACdwQAAAACdAAKUk9MRV9BRE1JTnQAEFNFUlZJQ0VfT1ZFUkxFQUZ4c3IADmphdmEudXRpbC5VVUlEvJkD95hthS8CAAJKAAxsZWFzdFNpZ0JpdHNKAAttb3N0U2lnQml0c3hwRERVVVVVVVURERERIiIzM3QABWpvcmlz"
    }
}
