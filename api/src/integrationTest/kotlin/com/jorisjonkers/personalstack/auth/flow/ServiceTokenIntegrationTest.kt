package com.jorisjonkers.personalstack.auth.flow

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.jorisjonkers.personalstack.auth.IntegrationTestBase
import com.jorisjonkers.personalstack.auth.domain.model.Role
import com.jorisjonkers.personalstack.auth.domain.model.ServicePermission
import com.jorisjonkers.personalstack.auth.domain.model.User
import com.jorisjonkers.personalstack.auth.domain.model.UserId
import com.jorisjonkers.personalstack.auth.domain.port.UserRepository
import com.jorisjonkers.personalstack.auth.infrastructure.security.AuthenticatedUser
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.time.Instant
import java.util.UUID

class ServiceTokenIntegrationTest : IntegrationTestBase() {
    @Autowired
    private lateinit var context: WebApplicationContext

    @Autowired
    private lateinit var userRepository: UserRepository

    private lateinit var mockMvc: MockMvc

    private val objectMapper = jacksonObjectMapper()

    private lateinit var testUser: User

    @BeforeEach
    fun setUp() {
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(context)
                .apply<DefaultMockMvcBuilder>(springSecurity())
                .build()

        val suffix = UUID.randomUUID().toString().take(6)
        testUser =
            User(
                id = UserId(UUID.randomUUID()),
                username = "svc_token_$suffix",
                email = "svc_token_$suffix@example.com",
                firstName = "",
                lastName = "",
                role = Role.USER,
                emailConfirmed = true,
                totpEnabled = false,
                createdAt = Instant.now(),
                updatedAt = Instant.now(),
            )
        userRepository.create(testUser, "\$2a\$10\$hash")
    }

    private fun authenticatedAs(userId: UserId) =
        user(
            AuthenticatedUser.of(
                userId = userId,
                username = testUser.username,
                roles = listOf("ROLE_USER"),
            ),
        )

    @Test
    fun `mint returns 401 without a session`() {
        mockMvc
            .post("/api/v1/auth/service-tokens") {
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = """{"service":"MEMORY_API","label":"laptop"}"""
            }.andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `mint returns 400 when the caller does not hold the grant`() {
        mockMvc
            .post("/api/v1/auth/service-tokens") {
                with(authenticatedAs(testUser.id))
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = """{"service":"MEMORY_API","label":"laptop"}"""
            }.andExpect {
                status { isBadRequest() }
            }
    }

    @Test
    fun `mint returns 201 with a raw token once the caller holds the grant`() {
        userRepository.saveServicePermissions(testUser.id, setOf(ServicePermission.MEMORY_API))

        mockMvc
            .post("/api/v1/auth/service-tokens") {
                with(authenticatedAs(testUser.id))
                with(csrf())
                contentType = MediaType.APPLICATION_JSON
                content = """{"service":"MEMORY_API","label":"macbook-claude-code"}"""
            }.andExpect {
                status { isCreated() }
                jsonPath("$.token") { value(org.hamcrest.Matchers.startsWith("smt_")) }
                jsonPath("$.service") { value("MEMORY_API") }
                jsonPath("$.label") { value("macbook-claude-code") }
            }
    }

    @Test
    fun `minted token authorizes verify for its own host but not the other memory host`() {
        userRepository.saveServicePermissions(testUser.id, setOf(ServicePermission.MEMORY_API))
        val token = mintToken(testUser.id, "MEMORY_API", "laptop")

        mockMvc
            .get("/api/v1/auth/verify") {
                header("Authorization", "Bearer $token")
                header("X-Forwarded-Host", "memory-api.jorisjonkers.dev")
            }.andExpect {
                status { isOk() }
                header { string("X-User-Roles", "SERVICE_MEMORY_API") }
            }

        mockMvc
            .get("/api/v1/auth/verify") {
                header("Authorization", "Bearer $token")
                header("X-Forwarded-Host", "memory-mcp.jorisjonkers.dev")
            }.andExpect {
                status { isForbidden() }
            }
    }

    @Test
    fun `a bearer call does not create a server session`() {
        userRepository.saveServicePermissions(testUser.id, setOf(ServicePermission.MEMORY_MCP))
        val token = mintToken(testUser.id, "MEMORY_MCP", "laptop")

        val result =
            mockMvc
                .get("/api/v1/auth/verify") {
                    header("Authorization", "Bearer $token")
                    header("X-Forwarded-Host", "memory-mcp.jorisjonkers.dev")
                }.andExpect { status { isOk() } }
                .andReturn()

        assert(result.request.getSession(false) == null) {
            "Expected no session to be created for a bearer-token verify call"
        }
    }

    @Test
    fun `revoked token no longer authorizes verify`() {
        userRepository.saveServicePermissions(testUser.id, setOf(ServicePermission.MEMORY_API))
        val mintResult =
            mockMvc
                .post("/api/v1/auth/service-tokens") {
                    with(authenticatedAs(testUser.id))
                    with(csrf())
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"service":"MEMORY_API","label":"laptop"}"""
                }.andReturn()
        val json = objectMapper.readTree(mintResult.response.contentAsString)
        val token = json["token"].asText()
        val id = json["id"].asText()

        mockMvc
            .delete("/api/v1/auth/service-tokens/$id") {
                with(authenticatedAs(testUser.id))
                with(csrf())
            }.andExpect { status { isNoContent() } }

        mockMvc
            .get("/api/v1/auth/verify") {
                header("Authorization", "Bearer $token")
                header("X-Forwarded-Host", "memory-api.jorisjonkers.dev")
            }.andExpect {
                status { is3xxRedirection() }
            }
    }

    @Test
    fun `revoke returns 404 for a token belonging to another user`() {
        userRepository.saveServicePermissions(testUser.id, setOf(ServicePermission.MEMORY_API))
        val mintResult =
            mockMvc
                .post("/api/v1/auth/service-tokens") {
                    with(authenticatedAs(testUser.id))
                    with(csrf())
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"service":"MEMORY_API","label":"laptop"}"""
                }.andReturn()
        val json = objectMapper.readTree(mintResult.response.contentAsString)
        val token = json["token"].asText()
        val id = json["id"].asText()

        mockMvc
            .delete("/api/v1/auth/service-tokens/$id") {
                with(authenticatedAs(UserId(UUID.randomUUID())))
                with(csrf())
            }.andExpect { status { isNotFound() } }

        // The original token, minted for testUser, must still work.
        mockMvc
            .get("/api/v1/auth/verify") {
                header("Authorization", "Bearer $token")
                header("X-Forwarded-Host", "memory-api.jorisjonkers.dev")
            }.andExpect { status { isOk() } }
    }

    private fun mintToken(
        userId: UserId,
        service: String,
        label: String,
    ): String {
        val result =
            mockMvc
                .post("/api/v1/auth/service-tokens") {
                    with(authenticatedAs(userId))
                    with(csrf())
                    contentType = MediaType.APPLICATION_JSON
                    content = """{"service":"$service","label":"$label"}"""
                }.andReturn()
        return objectMapper.readTree(result.response.contentAsString)["token"].asText()
    }
}
