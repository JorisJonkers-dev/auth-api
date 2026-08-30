package com.jorisjonkers.personalstack.auth.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * The JWK Set endpoint must stay publicly readable.
 *
 * It publishes public signing keys, and every relying party fetches it to verify
 * an id_token signature without holding a session. When it was gated behind
 * `anyRequest().authenticated()`, Vault's OIDC login exchanged its authorization
 * code successfully and then failed verification, because fetching the keys
 * redirected to /login and returned the auth-ui SPA:
 *
 *     failed to verify signature: fetching keys oidc: failed to decode keys:
 *     expected Content-Type = application/json, got "text/html"
 *
 * The failure was invisible for months: JWKS is cached by relying parties after
 * one successful fetch, so it only surfaces on a cache expiry or a restart.
 */
class AuthorizationServerPublicEndpointsTest {
    @Test
    fun `the advertised jwk set endpoint is one of the public endpoints`() {
        // The bug this guards is a rename: moving jwkSetEndpoint without adding
        // the new path to the public list restores the exact outage above, and
        // nothing else in the suite would notice.
        assertThat(AuthorizationServerConfig.PUBLIC_OAUTH2_ENDPOINTS)
            .contains(AuthorizationServerConfig.JWK_SET_ENDPOINT)
    }

    @Test
    fun `the jwk set endpoint sits under the authorization server security matcher`() {
        // The chain only matches /api/oauth2/**, /api/userinfo, /api/connect/logout
        // and /.well-known/**. A public endpoint outside those prefixes would be
        // permitted in a chain that never sees the request.
        assertThat(AuthorizationServerConfig.PUBLIC_OAUTH2_ENDPOINTS)
            .allSatisfy { endpoint ->
                assertThat(endpoint).startsWith("/api/oauth2/")
            }
    }

    @Test
    fun `no token-issuing or user-facing endpoint is public`() {
        // permitAll on any of these would hand out tokens or user data without a
        // session. Only key material belongs in the public list.
        assertThat(AuthorizationServerConfig.PUBLIC_OAUTH2_ENDPOINTS)
            .doesNotContain(
                "/api/oauth2/authorize",
                "/api/oauth2/token",
                "/api/oauth2/revoke",
                "/api/oauth2/introspect",
                "/api/userinfo",
            )
    }
}
