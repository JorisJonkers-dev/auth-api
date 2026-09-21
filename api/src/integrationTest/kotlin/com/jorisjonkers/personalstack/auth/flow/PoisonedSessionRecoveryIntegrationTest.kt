package com.jorisjonkers.personalstack.auth.flow

import com.jorisjonkers.personalstack.auth.IntegrationTestBase
import com.jorisjonkers.personalstack.auth.jooq.tables.AppUser.APP_USER
import com.jorisjonkers.personalstack.auth.jooq.tables.EmailConfirmationToken.EMAIL_CONFIRMATION_TOKEN
import jakarta.servlet.Filter
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.util.UUID

/**
 * A session this build cannot read must not lock the account out.
 *
 * `AuthenticatedUser` is JDK-serialized into Valkey as part of the stored
 * `SecurityContext`, and adding `viaServiceToken` changed its derived
 * `serialVersionUID`. Every session written before that deploy then threw
 *
 *   InvalidClassException: ...AuthenticatedUser; local class incompatible
 *
 * out of `SessionRepositoryFilter`, while Spring Security loaded the deferred
 * context — before any controller, so the caller saw a bodyless 401 and the
 * request log stayed empty. Signing in again hit the same row, because the
 * browser kept presenting the same `SESSION` cookie. Only clearing cookies
 * escaped it, which is not something users can be asked to do.
 *
 * Pinning the id fixes the sessions written before the break. This covers the
 * other half: a row that cannot be read for any reason is dropped and the
 * request continues anonymously, so the next sign-in succeeds while holding the
 * poisoned cookie.
 */
class PoisonedSessionRecoveryIntegrationTest : IntegrationTestBase() {
    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired
    private lateinit var dsl: DSLContext

    @Autowired
    private lateinit var redisConnectionFactory: RedisConnectionFactory

    @Autowired
    @Qualifier("springSessionRepositoryFilter")
    private lateinit var sessionRepositoryFilter: Filter

    private lateinit var mockMvc: MockMvc

    private val password = "Str0ng-Passw0rd!"

    @BeforeEach
    fun setUp() {
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(context)
                .addFilters<DefaultMockMvcBuilder>(sessionRepositoryFilter)
                .apply<DefaultMockMvcBuilder>(springSecurity())
                .build()
    }

    @Test
    fun `an unreadable session does not block signing in again`() {
        val username = "poison_${UUID.randomUUID().toString().take(8)}"
        registerUser(username)
        confirmEmail(username)

        val sessionCookie = signIn(username)
        mockMvc
            .get("/api/v1/auth/me") { cookie(sessionCookie) }
            .andExpect { status { isOk() } }

        val poisoned = poisonEveryStoredSession()
        assertThat(poisoned).isPositive()

        // The old behaviour: this threw out of the filter and answered 401
        // forever, sign-in included.
        mockMvc
            .get("/api/v1/auth/me") { cookie(sessionCookie) }
            .andExpect { status { isUnauthorized() } }

        mockMvc
            .post("/api/v1/auth/session-login") {
                cookie(sessionCookie)
                contentType = MediaType.APPLICATION_JSON
                content = """{"username": "$username", "password": "$password"}"""
            }.andExpect { status { isOk() } }
    }

    private fun signIn(username: String): jakarta.servlet.http.Cookie {
        val result =
            mockMvc
                .post("/api/v1/auth/session-login") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"username": "$username", "password": "$password"}"""
                }.andExpect { status { isOk() } }
                .andReturn()
        return result.response.getCookie(SESSION_COOKIE)
            ?: error("sign-in issued no $SESSION_COOKIE cookie")
    }

    /**
     * Overwrites the stored `SecurityContext` with a byte array that opens as a
     * Java object stream and then fails, which is the shape of a class-version
     * mismatch as far as the deserializer is concerned.
     */
    private fun poisonEveryStoredSession(): Int {
        val unreadable = byteArrayOf(0xAC.toByte(), 0xED.toByte(), 0x00, 0x05, 0x7F, 0x7F, 0x7F)
        redisConnectionFactory.connection.use { connection ->
            val keys = connection.keyCommands().keys("auth-api:sessions:*".toByteArray()).orEmpty()
            keys.forEach { key ->
                connection.hashCommands().hSet(key, SECURITY_CONTEXT_FIELD.toByteArray(), unreadable)
            }
            return keys.size
        }
    }

    private fun registerUser(username: String) {
        mockMvc
            .post("/api/v1/users/register") {
                contentType = MediaType.APPLICATION_JSON
                content =
                    """
                    {
                      "username": "$username",
                      "email": "$username@example.com",
                      "firstName": "Test",
                      "lastName": "User",
                      "password": "$password"
                    }
                    """.trimIndent()
            }.andExpect { status { isCreated() } }
    }

    private fun confirmEmail(username: String) {
        val userId =
            dsl
                .select(APP_USER.ID)
                .from(APP_USER)
                .where(APP_USER.USERNAME.eq(username))
                .fetchOne(APP_USER.ID)!!
        val token =
            dsl
                .select(EMAIL_CONFIRMATION_TOKEN.TOKEN)
                .from(EMAIL_CONFIRMATION_TOKEN)
                .where(EMAIL_CONFIRMATION_TOKEN.USER_ID.eq(userId))
                .fetchOne(EMAIL_CONFIRMATION_TOKEN.TOKEN)!!
        mockMvc
            .get("/api/v1/auth/confirm-email") { param("token", token) }
            .andExpect { status { isOk() } }
    }

    private companion object {
        const val SESSION_COOKIE = "SESSION"
        const val SECURITY_CONTEXT_FIELD = "sessionAttr:SPRING_SECURITY_CONTEXT"
    }
}
