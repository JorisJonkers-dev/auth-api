package com.jorisjonkers.personalstack.auth.flow

import com.jorisjonkers.personalstack.auth.IntegrationTestBase
import com.jorisjonkers.personalstack.auth.config.CacheConfig.Companion.CACHE_USERS_BY_ID
import com.jorisjonkers.personalstack.auth.domain.model.ServicePermission
import com.jorisjonkers.personalstack.auth.domain.model.User
import com.jorisjonkers.personalstack.auth.domain.model.UserId
import com.jorisjonkers.personalstack.auth.domain.port.UserRepository
import com.jorisjonkers.personalstack.auth.jooq.tables.AppUser.APP_USER
import com.jorisjonkers.personalstack.auth.jooq.tables.EmailConfirmationToken.EMAIL_CONFIRMATION_TOKEN
import jakarta.servlet.DispatcherType
import jakarta.servlet.Filter
import jakarta.servlet.RequestDispatcher
import jakarta.servlet.http.Cookie
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.containsString
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.CacheManager
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY
import org.springframework.session.Session
import org.springframework.session.SessionRepository
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID

// A session outlives its sign-in by up to 30 days, so roles changed later must still reach it.
// Sessions here go through the real Valkey-backed SessionRepositoryFilter, as in production.
class SessionRolesRefreshIntegrationTest : IntegrationTestBase() {
    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired
    private lateinit var dsl: DSLContext

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var cacheManager: CacheManager

    @Autowired
    private lateinit var sessionRepository: SessionRepository<out Session>

    @Autowired
    @Qualifier("springSessionRepositoryFilter")
    private lateinit var sessionRepositoryFilter: Filter

    private lateinit var mockMvc: MockMvc

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
    fun `a service permission granted after sign-in reaches the open session`() {
        val (userId, session) = signedInUser()
        verify(session, "tribelt.jorisjonkers.dev").andExpect { status { isForbidden() } }
        authorizeTribelt(session).andExpect { status { isForbidden() } }

        userRepository.saveServicePermissions(userId, setOf(ServicePermission.TRIBELT))

        verify(session, "tribelt.jorisjonkers.dev").andExpect {
            status { isOk() }
            header { string("X-User-Roles", containsString("SERVICE_TRIBELT")) }
        }
        assertThat(authorizeTribelt(session).andReturn().response.status).isNotIn(401, 403)
    }

    // Tribelt runs its own OIDC flow, so the browser reaches authorize with no forward-auth request before it.
    @Test
    fun `a service permission granted after sign-in reaches the authorize endpoint on its own`() {
        val (userId, session) = signedInUser()
        authorizeTribelt(session).andExpect { status { isForbidden() } }

        userRepository.saveServicePermissions(userId, setOf(ServicePermission.TRIBELT))

        assertThat(authorizeTribelt(session).andReturn().response.status).isNotIn(401, 403)
        assertThat(storedAuthorities(session)).contains("SERVICE_TRIBELT")
    }

    @Test
    fun `a grant evicts the cached user the session refresh reads`() {
        val (userId, session) = signedInUser()
        verify(session, "tribelt.jorisjonkers.dev").andExpect { status { isForbidden() } }
        assertThat(cachedUser(userId)?.servicePermissions)
            .describedAs("the refresh must read through users.byId, so a stale entry is what it would see")
            .isEmpty()

        userRepository.saveServicePermissions(userId, setOf(ServicePermission.TRIBELT))
        assertThat(cachedUser(userId)).isNull()

        verify(session, "tribelt.jorisjonkers.dev").andExpect { status { isOk() } }
        assertThat(cachedUser(userId)?.servicePermissions).containsExactly(ServicePermission.TRIBELT)
    }

    @Test
    fun `refreshed roles are written back to the stored session and keep its sign-in factors`() {
        val (userId, session) = signedInUser()
        userRepository.saveServicePermissions(userId, setOf(ServicePermission.TRIBELT))

        verify(session, "tribelt.jorisjonkers.dev").andExpect { status { isOk() } }

        val authorities = storedAuthorities(session)
        assertThat(authorities).contains("ROLE_USER", "SERVICE_TRIBELT")
        assertThat(authorities).anyMatch { it.startsWith("FACTOR_") }
    }

    @Test
    fun `a service permission revoked after sign-in leaves the open session`() {
        val (userId, session) = signedInUser()
        userRepository.saveServicePermissions(userId, setOf(ServicePermission.GRAFANA))
        verify(session, "grafana.jorisjonkers.dev").andExpect { status { isOk() } }

        userRepository.saveServicePermissions(userId, emptySet())

        verify(session, "grafana.jorisjonkers.dev").andExpect { status { isForbidden() } }
        assertThat(storedAuthorities(session)).doesNotContain("SERVICE_GRAFANA")
    }

    // sendError() re-dispatches to /error without a security context; a 403 must stay a 403.
    @Test
    fun `an error dispatch keeps its status instead of becoming 401`() {
        mockMvc
            .get("/error") {
                with {
                    it.dispatcherType = DispatcherType.ERROR
                    it.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, 403)
                    it.setAttribute(RequestDispatcher.ERROR_REQUEST_URI, "/api/oauth2/authorize")
                    it
                }
            }.andExpect { status { isForbidden() } }
    }

    private fun signedInUser(): Pair<UserId, Cookie> {
        val username = "roles_${UUID.randomUUID().toString().take(8)}"
        val password = "securepass123"
        mockMvc
            .post("/api/v1/users/register") {
                contentType = MediaType.APPLICATION_JSON
                content =
                    """{"username":"$username","email":"$username@example.com",""" +
                    """"firstName":"Test","lastName":"User","password":"$password"}"""
            }.andExpect { status { isCreated() } }

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
        mockMvc.get("/api/v1/auth/confirm-email") { param("token", token) }.andExpect { status { isOk() } }

        val login =
            mockMvc
                .post("/api/v1/auth/session-login") {
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"username":"$username","password":"$password"}"""
                }.andExpect { status { isOk() } }
                .andReturn()
        val cookie = login.response.getCookie(SESSION_COOKIE) ?: error("sign-in issued no $SESSION_COOKIE cookie")
        return UserId(userId) to cookie
    }

    private fun cachedUser(userId: UserId): User? = cacheManager.getCache(CACHE_USERS_BY_ID)?.get(userId.value, User::class.java)

    private fun storedAuthorities(session: Cookie): List<String> {
        val sessionId = String(Base64.getDecoder().decode(session.value))
        val stored = sessionRepository.findById(sessionId) ?: error("session $sessionId is not stored")
        val securityContext = stored.getAttribute<SecurityContext>(SPRING_SECURITY_CONTEXT_KEY)
        return securityContext.authentication!!.authorities.mapNotNull { it.authority }
    }

    private fun verify(
        session: Cookie,
        host: String,
    ): ResultActionsDsl =
        mockMvc.get("/api/v1/auth/verify") {
            header("X-Forwarded-Host", host)
            cookie(session)
        }

    private fun authorizeTribelt(session: Cookie): ResultActionsDsl {
        val verifier = UUID.randomUUID().toString() + UUID.randomUUID().toString()
        val challenge =
            Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()),
            )
        return mockMvc.get("/api/oauth2/authorize") {
            param("response_type", "code")
            param("client_id", "tribelt")
            param("redirect_uri", "https://tribelt.jorisjonkers.test/auth/callback")
            param("scope", "openid profile email")
            param("code_challenge", challenge)
            param("code_challenge_method", "S256")
            param("state", "roles-refresh")
            accept = MediaType.TEXT_HTML
            cookie(session)
        }
    }

    private companion object {
        const val SESSION_COOKIE = "SESSION"
    }
}
