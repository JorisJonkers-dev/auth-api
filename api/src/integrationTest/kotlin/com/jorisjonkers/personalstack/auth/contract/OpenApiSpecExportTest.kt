package com.jorisjonkers.personalstack.auth.contract

import com.jorisjonkers.personalstack.auth.application.command.DeleteUserCommandHandler
import com.jorisjonkers.personalstack.auth.application.command.EnrollTotpCommandHandler
import com.jorisjonkers.personalstack.auth.application.command.MintServiceTokenCommandHandler
import com.jorisjonkers.personalstack.auth.application.command.UpdateUserRoleCommandHandler
import com.jorisjonkers.personalstack.auth.application.command.UpdateUserServicePermissionsCommandHandler
import com.jorisjonkers.personalstack.auth.application.query.GetAllUsersQueryHandler
import com.jorisjonkers.personalstack.auth.application.query.GetUserQueryService
import com.jorisjonkers.personalstack.auth.config.OpenApiConfig
import com.jorisjonkers.personalstack.auth.config.ProblemResponseConfig
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
import com.jorisjonkers.personalstack.auth.infrastructure.web.ServiceTokenController
import com.jorisjonkers.personalstack.auth.infrastructure.web.SessionLoginController
import com.jorisjonkers.personalstack.auth.infrastructure.web.TotpController
import com.jorisjonkers.personalstack.auth.infrastructure.web.UserRegistrationController
import com.jorisjonkers.personalstack.common.command.CommandBus
import com.jorisjonkers.personalstack.common.test.openapi.OpenApiSliceExporter
import com.jorisjonkers.personalstack.common.test.openapi.OpenApiWebMvcSliceConfiguration
import com.jorisjonkers.personalstack.common.web.GlobalExceptionHandler
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.web.bind.annotation.RestController
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
        ServiceTokenController::class,
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
        ProblemResponseConfig::class,
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
        ServiceTokenController::class,
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

    // A document-level `security` applies to every operation, so generated
    // clients attached a bearer token to sign-in and registration. The
    // declaration now sits on the operations that actually take one.
    @Test
    fun `the document declares no blanket security requirement`() {
        mockMvc
            .perform(get("/api/v1/api-docs"))
            .andExpect(jsonPath("$['security']").doesNotExist())
    }

    @Test
    fun `public endpoints declare no security requirement`() {
        val response = mockMvc.perform(get("/api/v1/api-docs"))
        for (operation in PUBLIC_OPERATIONS) {
            response.andExpect(jsonPath("$['paths']['${operation.first}']['${operation.second}']['security']").doesNotExist())
        }
    }

    @Test
    fun `protected endpoints declare bearerAuth`() {
        val response = mockMvc.perform(get("/api/v1/api-docs"))
        for (operation in PROTECTED_OPERATIONS) {
            response.andExpect(
                jsonPath("$['paths']['${operation.first}']['${operation.second}']['security'][0]['bearerAuth']").exists(),
            )
        }
    }

    // #42. Every error this service returns is an RFC 7807 document; the spec
    // used to say nothing about it, so a generated client typed each failure as
    // an opaque body.
    @Test
    fun `error responses are declared as problem json`() {
        mockMvc
            .perform(get("/api/v1/api-docs"))
            .andExpect(jsonPath("$['components']['schemas']['ProblemDetail']").exists())
            .andExpect(jsonPath("$['components']['schemas']['FieldError']").exists())
            .andExpect(
                jsonPath(
                    "$['paths']['/api/v1/auth/session-login']['post']['responses']['400']" +
                        "['content']['application/problem+json']['schema']['\$ref']",
                ).value("#/components/schemas/ProblemDetail"),
            ).andExpect(
                jsonPath(
                    "$['paths']['/api/v1/users/register']['post']['responses']['422']" +
                        "['content']['application/problem+json']",
                ).exists(),
            ).andExpect(
                jsonPath(
                    "$['paths']['/api/v1/admin/users']['get']['responses']['403']" +
                        "['content']['application/problem+json']",
                ).exists(),
            )
    }

    // The two rejections that carry no body must not claim one: Spring Security's
    // entry point and the forward-auth service gate both write a bare status.
    @Test
    fun `bodyless rejections are declared without content`() {
        mockMvc
            .perform(get("/api/v1/api-docs"))
            .andExpect(jsonPath("$['paths']['/api/v1/auth/me']['get']['responses']['401']").exists())
            .andExpect(jsonPath("$['paths']['/api/v1/auth/me']['get']['responses']['401']['content']").doesNotExist())
            .andExpect(jsonPath("$['paths']['/api/v1/auth/verify']['get']['responses']['403']['content']").doesNotExist())
    }

    // A public endpoint must not advertise a 401 it cannot return.
    @Test
    fun `public endpoints declare no unauthorized response`() {
        mockMvc
            .perform(get("/api/v1/api-docs"))
            .andExpect(jsonPath("$['paths']['/api/v1/auth/session-login']['post']['responses']['401']").doesNotExist())
            .andExpect(jsonPath("$['paths']['/api/v1/health']['get']['responses']['401']").doesNotExist())
    }

    // This slice enumerates its controllers, so a new @RestController is absent
    // from the published contract until someone remembers to add it here — and
    // the drift gate cannot see the gap, because it compares the committed spec
    // against this same slice. ServiceTokenController shipped that way.
    @Test
    fun `every RestController in the application is part of this slice`() {
        val scanner = ClassPathScanningCandidateComponentProvider(false)
        scanner.addIncludeFilter(AnnotationTypeFilter(RestController::class.java))
        val discovered =
            scanner
                .findCandidateComponents("com.jorisjonkers.personalstack.auth.infrastructure.web")
                .mapNotNull { it.beanClassName }
                .toSet()
        val declared =
            OpenApiSpecExportTest::class.java
                .getAnnotation(WebMvcTest::class.java)
                .controllers
                .map { it.java.name }
                .toSet()

        assertThat(discovered).isNotEmpty()
        assertThat(discovered - declared)
            .describedAs("controllers missing from the export slice, so absent from the published spec")
            .isEmpty()
    }

    private fun resolveOpenApiSpecPath(): Path {
        val override = System.getProperty("openapi.spec.output")
        if (override != null) {
            return Paths.get(override)
        }
        return Paths.get(System.getProperty("user.dir")).resolve("client-spec/openapi/auth-api.json")
    }

    companion object {
        private val PUBLIC_OPERATIONS =
            listOf(
                "/api/v1/auth/session-login" to "post",
                "/api/v1/auth/login" to "post",
                "/api/v1/auth/totp-challenge" to "post",
                "/api/v1/auth/refresh" to "post",
                "/api/v1/auth/confirm-email" to "get",
                "/api/v1/auth/resend-confirmation" to "post",
                "/api/v1/auth/forgot-password" to "post",
                "/api/v1/auth/reset-password" to "post",
                "/api/v1/users/register" to "post",
                "/api/v1/health" to "get",
            )

        private val PROTECTED_OPERATIONS =
            listOf(
                "/api/v1/auth/me" to "get",
                "/api/v1/auth/verify" to "get",
                "/api/v1/users/me" to "patch",
                "/api/v1/admin/users" to "get",
                "/api/v1/totp/enroll" to "post",
                "/api/v1/auth/change-password" to "post",
            )
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
        fun mintServiceTokenCommandHandler(): MintServiceTokenCommandHandler = mockk(relaxed = true)

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
