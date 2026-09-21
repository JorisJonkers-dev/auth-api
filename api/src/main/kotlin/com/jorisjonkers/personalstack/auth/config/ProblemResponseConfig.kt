package com.jorisjonkers.personalstack.auth.config

import com.jorisjonkers.personalstack.common.web.ProblemDetail
import io.swagger.v3.core.converter.AnnotatedType
import io.swagger.v3.core.converter.ModelConverters
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.util.AntPathMatcher

private const val PROBLEM_JSON = "application/problem+json"
private const val PROBLEM_SCHEMA = "ProblemDetail"
private const val PROBLEM_SCHEMA_REF = "#/components/schemas/$PROBLEM_SCHEMA"

private const val BAD_REQUEST = "400"
private const val UNAUTHORIZED = "401"
private const val FORBIDDEN = "403"
private const val NOT_FOUND = "404"
private const val UNPROCESSABLE = "422"
private const val SERVER_ERROR = "500"

/**
 * Declares each operation's failure modes in the published contract.
 *
 * Every error this service returns is already an RFC 7807 document — that is
 * what `GlobalExceptionHandler` and `AuthExceptionHandler` produce — but the
 * spec said nothing about it, so generated clients typed every failure as an
 * opaque wildcard-media-type body. The statuses below are not a template: each is attached
 * only where a handler can actually produce it, and the two bodyless
 * rejections are declared without content because that is what the caller
 * receives.
 *
 * | status | source | body |
 * |---|---|---|
 * | 400 | `DomainException`, `IllegalArgumentException`, unreadable body, path-variable type mismatch | problem+json |
 * | 401 | `HttpStatusEntryPoint` on the application chain | **none** |
 * | 403 | `AccessDenied` → `AuthExceptionHandler` (admin); `/auth/verify` service gate | problem+json / **none** |
 * | 404 | `NotFoundException` from a command handler or `GetUserQueryService` | problem+json |
 * | 422 | bean-validation failures | problem+json |
 * | 500 | the catch-all handler | problem+json |
 */
@Configuration
class ProblemResponseConfig {
    @Bean
    fun problemResponseCustomizer(): OpenApiCustomizer =
        OpenApiCustomizer { openApi ->
            registerProblemSchemas(openApi)
            openApi.paths?.forEach { (path, pathItem) ->
                pathItem.readOperationsMap().forEach { (method, operation) ->
                    declareErrors(path, method.name, operation)
                }
            }
        }

    private fun registerProblemSchemas(openApi: OpenAPI) {
        val resolved = ModelConverters.getInstance().readAll(AnnotatedType(ProblemDetail::class.java))
        val components = openApi.components ?: return
        resolved.forEach { (name, schema) ->
            @Suppress("UNCHECKED_CAST")
            components.addSchemas(name, schema as Schema<Any>)
        }
    }

    private fun declareErrors(
        path: String,
        method: String,
        operation: Operation,
    ) {
        val responses = operation.responses ?: return
        statusesFor(path, method, operation).forEach { (status, carriesBody) ->
            if (!responses.containsKey(status)) {
                responses.addApiResponse(status, response(status, carriesBody))
            }
        }
    }

    /** Status to "carries a problem+json body", in the order they appear in the document. */
    private fun statusesFor(
        path: String,
        method: String,
        operation: Operation,
    ): List<Pair<String, Boolean>> =
        buildList {
            val operationId = "$method $path"
            val hasBody = operation.requestBody != null
            // A UUID path variable that does not parse is a type mismatch, which
            // Spring answers with 400 before the controller runs.
            val hasPathVariable = path.contains('{')
            if (hasBody || hasPathVariable || operationId in DOMAIN_ERROR_OPERATIONS) {
                add(BAD_REQUEST to true)
            }
            if (!isPublic(path)) {
                // HttpStatusEntryPoint writes a status and nothing else.
                add(UNAUTHORIZED to false)
            }
            forbiddenFor(path)?.let { add(it) }
            if (operationId in NOT_FOUND_OPERATIONS) {
                add(NOT_FOUND to true)
            }
            // Bean validation only runs against an @Valid body here; a path or
            // query parameter that is wrong fails earlier, as a 400.
            if (hasBody) {
                add(UNPROCESSABLE to true)
            }
            add(SERVER_ERROR to true)
        }

    private fun forbiddenFor(path: String): Pair<String, Boolean>? =
        when {
            MATCHER.match(ADMIN_PATHS, path) -> FORBIDDEN to true
            path == FORWARD_AUTH_PATH -> FORBIDDEN to false
            else -> null
        }

    private fun isPublic(path: String): Boolean = PublicEndpoints.PATTERNS.any { MATCHER.match(it, path) }

    private fun response(
        status: String,
        carriesBody: Boolean,
    ): ApiResponse {
        val description = DESCRIPTIONS.getValue(status)
        if (!carriesBody) {
            return ApiResponse().description(description)
        }
        return ApiResponse()
            .description(description)
            .content(
                Content().addMediaType(
                    PROBLEM_JSON,
                    MediaType().schema(Schema<Any>().`$ref`(PROBLEM_SCHEMA_REF)),
                ),
            )
    }

    private companion object {
        val MATCHER = AntPathMatcher()

        const val ADMIN_PATHS = "/api/v1/admin/**"
        const val FORWARD_AUTH_PATH = "/api/v1/auth/verify"

        /**
         * Operations with no request body whose handlers still raise a
         * `DomainException`, which maps to 400: an invalid or expired
         * confirmation token, and TOTP that is already enrolled.
         */
        val DOMAIN_ERROR_OPERATIONS =
            setOf(
                "GET /api/v1/auth/confirm-email",
                "POST /api/v1/totp/enroll",
            )

        /**
         * Operations whose handlers throw `NotFoundException`, which maps to 404.
         * `UserNotFoundException` is a plain `DomainException` and maps to 400, so
         * the endpoints that raise that one are deliberately absent here.
         */
        val NOT_FOUND_OPERATIONS =
            setOf(
                "GET /api/v1/admin/users/{id}",
                "DELETE /api/v1/admin/users/{id}",
                "PATCH /api/v1/admin/users/{id}/role",
                "PUT /api/v1/admin/users/{id}/services",
                "GET /api/v1/auth/me",
                "GET /api/v1/users/me",
                "PATCH /api/v1/users/me",
                "POST /api/v1/totp/enroll",
                "POST /api/v1/totp/verify",
                "POST /api/v1/auth/service-tokens",
                "DELETE /api/v1/auth/service-tokens/{id}",
            )

        val DESCRIPTIONS =
            mapOf(
                BAD_REQUEST to "Request rejected on its own terms — see `type` for the specific code",
                UNAUTHORIZED to "No authenticated session or token. Empty body.",
                FORBIDDEN to "Authenticated but not permitted",
                NOT_FOUND to "The referenced resource does not exist",
                UNPROCESSABLE to "Validation failed — `errors` carries the offending fields",
                SERVER_ERROR to "Unhandled failure — correlate on `traceId`",
            )
    }
}
