package com.jorisjonkers.personalstack.auth.config

/**
 * Endpoints that answer without authentication.
 *
 * One list, two consumers: [SecurityConfig] builds the public filter chain from
 * it, and [OpenApiConfig] decides from it which operations declare `bearerAuth`.
 * Kept here so a new public endpoint cannot be opened in the filter chain while
 * the published contract keeps telling clients to send a token to it.
 */
object PublicEndpoints {
    val PATTERNS =
        arrayOf(
            "/api/actuator/**",
            "/api/v1/health",
            "/api/v1/api-docs/**",
            "/api/v1/swagger-ui/**",
            "/api/v1/users/register",
            "/api/v1/auth/login",
            "/api/v1/auth/totp-challenge",
            "/api/v1/auth/refresh",
            "/api/v1/auth/confirm-email",
            "/api/v1/auth/resend-confirmation",
            "/api/v1/auth/session-login",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
        )
}
