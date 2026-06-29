package com.jorisjonkers.personalstack.auth.contract

import com.jorisjonkers.personalstack.auth.application.command.DeleteUserCommandHandler
import com.jorisjonkers.personalstack.auth.application.command.EnrollTotpCommandHandler
import com.jorisjonkers.personalstack.auth.application.command.UpdateUserRoleCommandHandler
import com.jorisjonkers.personalstack.auth.application.command.UpdateUserServicePermissionsCommandHandler
import com.jorisjonkers.personalstack.auth.application.query.GetAllUsersQueryHandler
import com.jorisjonkers.personalstack.auth.application.query.GetUserQueryService
import com.jorisjonkers.personalstack.auth.config.OpenApiConfig
import com.jorisjonkers.personalstack.auth.domain.port.PasswordEncoder
import com.jorisjonkers.personalstack.auth.domain.port.UserRepository
import com.jorisjonkers.personalstack.auth.domain.service.TotpService
import com.jorisjonkers.personalstack.auth.infrastructure.security.TokenService
import com.jorisjonkers.personalstack.auth.infrastructure.web.AdminController
import com.jorisjonkers.personalstack.auth.infrastructure.web.AuthExceptionHandler
import com.jorisjonkers.personalstack.auth.infrastructure.web.AuthVerificationController
import com.jorisjonkers.personalstack.auth.infrastructure.web.EmailConfirmationController
import com.jorisjonkers.personalstack.auth.infrastructure.web.HealthController
import com.jorisjonkers.personalstack.auth.infrastructure.web.LoginController
import com.jorisjonkers.personalstack.auth.infrastructure.web.LogoutController
import com.jorisjonkers.personalstack.auth.infrastructure.web.MeController
import com.jorisjonkers.personalstack.auth.infrastructure.web.PasswordController
import com.jorisjonkers.personalstack.auth.infrastructure.web.ProfileController
import com.jorisjonkers.personalstack.auth.infrastructure.web.SessionLoginController
import com.jorisjonkers.personalstack.auth.infrastructure.web.TotpController
import com.jorisjonkers.personalstack.auth.infrastructure.web.UserRegistrationController
import com.jorisjonkers.personalstack.common.command.CommandBus
import com.jorisjonkers.personalstack.common.test.openapi.OpenApiSliceExporter
import com.jorisjonkers.personalstack.common.test.openapi.OpenApiWebMvcSliceConfiguration
import com.jorisjonkers.personalstack.common.web.GlobalExceptionHandler
import io.mockk.mockk
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import java.nio.file.Path
import java.nio.file.Paths

@Tag("contract-export")
@WebMvcTest(
    controllers = [
        AdminController::class,
        AuthVerificationController::class,
        EmailConfirmationController::class,
        HealthController::class,
        LoginController::class,
        LogoutController::class,
        MeController::class,
        PasswordController::class,
        ProfileController::class,
        SessionLoginController::class,
        TotpController::class,
        UserRegistrationController::class,
    ],
    properties = [
        "springdoc.api-docs.enabled=true",
        "springdoc.api-docs.path=/api/v1/api-docs",
        "springdoc.writer-with-default-pretty-printer=true",
        "springdoc.writer-with-order-by-keys=true",
    ],
)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(
    classes = [
        OpenApiSpecExportTest.Application::class,
        OpenApiSpecExportTest.Collaborators::class,
        OpenApiWebMvcSliceConfiguration::class,
        OpenApiConfig::class,
        AuthExceptionHandler::class,
        GlobalExceptionHandler::class,
        AdminController::class,
        AuthVerificationController::class,
        EmailConfirmationController::class,
        HealthController::class,
        LoginController::class,
        LogoutController::class,
        MeController::class,
        PasswordController::class,
        ProfileController::class,
        SessionLoginController::class,
        TotpController::class,
        UserRegistrationController::class,
    ],
)
class OpenApiSpecExportTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `export OpenAPI spec to client spec`() {
        OpenApiSliceExporter.writeJson(mockMvc, resolveOpenApiSpecPath(), "/api/v1/api-docs")
    }

    @Test
    fun `auth browser and token endpoints are exported`() {
        mockMvc
            .perform(get("/api/v1/api-docs"))
            .andExpect(jsonPath("$['paths']['/api/v1/auth/login']").exists())
            .andExpect(jsonPath("$['paths']['/api/v1/auth/session-login']").exists())
            .andExpect(jsonPath("$['paths']['/api/v1/auth/verify']").exists())
            .andExpect(jsonPath("$['paths']['/api/v1/auth/me']").exists())
    }

    @Test
    fun `profile admin and totp endpoints are exported`() {
        mockMvc
            .perform(get("/api/v1/api-docs"))
            .andExpect(jsonPath("$['paths']['/api/v1/users/me']").exists())
            .andExpect(jsonPath("$['paths']['/api/v1/admin/users']").exists())
            .andExpect(jsonPath("$['paths']['/api/v1/totp/enroll']").exists())
            .andExpect(jsonPath("$['paths']['/api/v1/users/register']").exists())
    }

    private fun resolveOpenApiSpecPath(): Path {
        val override = System.getProperty("openapi.spec.output")
        if (override != null) {
            return Paths.get(override)
        }
        return Paths.get(System.getProperty("user.dir")).resolve("client-spec/openapi/auth-api.json")
    }

    @SpringBootConfiguration
    class Application

    @TestConfiguration(proxyBeanMethods = false)
    class Collaborators {
        @Bean
        fun commandBus(): CommandBus = mockk(relaxed = true)

        @Bean
        fun deleteUserCommandHandler(): DeleteUserCommandHandler = mockk(relaxed = true)

        @Bean
        fun enrollTotpCommandHandler(): EnrollTotpCommandHandler = mockk(relaxed = true)

        @Bean
        fun getAllUsersQueryHandler(): GetAllUsersQueryHandler = mockk(relaxed = true)

        @Bean
        fun getUserQueryService(): GetUserQueryService = mockk(relaxed = true)

        @Bean
        fun jwtDecoder(): JwtDecoder = mockk(relaxed = true)

        @Bean
        fun passwordEncoder(): PasswordEncoder = mockk(relaxed = true)

        @Bean
        fun tokenService(): TokenService = mockk(relaxed = true)

        @Bean
        fun totpService(): TotpService = mockk(relaxed = true)

        @Bean
        fun updateUserRoleCommandHandler(): UpdateUserRoleCommandHandler = mockk(relaxed = true)

        @Bean
        fun updateUserServicePermissionsCommandHandler(): UpdateUserServicePermissionsCommandHandler = mockk(relaxed = true)

        @Bean
        fun userRepository(): UserRepository = mockk(relaxed = true)
    }
}
