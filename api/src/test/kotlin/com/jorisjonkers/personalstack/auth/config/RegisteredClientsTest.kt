package com.jorisjonkers.personalstack.auth.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.oidc.OidcScopes
import java.util.UUID

class RegisteredClientsTest {
    @Test
    fun `app-native is a public PKCE client for Capacitor redirects`() {
        val client = buildAppNativeClient()

        assertThat(client.id).isEqualTo(UUID.nameUUIDFromBytes("app-native".toByteArray()).toString())
        assertThat(client.clientId).isEqualTo("app-native")
        assertThat(client.clientAuthenticationMethods).containsExactly(ClientAuthenticationMethod.NONE)
        assertThat(client.authorizationGrantTypes)
            .containsExactlyInAnyOrder(
                AuthorizationGrantType.AUTHORIZATION_CODE,
                AuthorizationGrantType.REFRESH_TOKEN,
            )
        assertThat(client.clientSettings.isRequireProofKey).isTrue
        assertThat(client.scopes)
            .containsExactlyInAnyOrder(
                OidcScopes.OPENID,
                OidcScopes.PROFILE,
                OidcScopes.EMAIL,
            )
        assertThat(client.redirectUris)
            .containsExactlyInAnyOrder(
                "app.jorisjonkers://callback",
                "capacitor://localhost/callback",
                "http://localhost/callback",
            )
        assertThat(client.postLogoutRedirectUris)
            .containsExactlyInAnyOrder(
                "app.jorisjonkers://callback",
                "capacitor://localhost/callback",
                "http://localhost/callback",
            )
    }

    @Test
    fun `hermes is a public PKCE client whose redirect matches the dashboard callback`() {
        val client = buildHermesClient()

        assertThat(client.id).isEqualTo(UUID.nameUUIDFromBytes("hermes".toByteArray()).toString())
        assertThat(client.clientId).isEqualTo("hermes")
        // Hermes' dashboard has no client-secret field, so a confidential
        // client could not authenticate at all.
        assertThat(client.clientAuthenticationMethods).containsExactly(ClientAuthenticationMethod.NONE)
        assertThat(client.clientSettings.isRequireProofKey).isTrue
        assertThat(client.authorizationGrantTypes)
            .containsExactlyInAnyOrder(
                AuthorizationGrantType.AUTHORIZATION_CODE,
                AuthorizationGrantType.REFRESH_TOKEN,
            )
        assertThat(client.scopes)
            .containsExactlyInAnyOrder(
                OidcScopes.OPENID,
                OidcScopes.PROFILE,
                OidcScopes.EMAIL,
            )
        // Hermes constructs the callback as <public_url>/auth/callback
        // verbatim. If these drift from HERMES_DASHBOARD_PUBLIC_URL in
        // fleet-infra, the login fails at the redirect with a mismatch the
        // dashboard reports only as a generic error.
        assertThat(client.redirectUris)
            .containsExactlyInAnyOrder(
                "https://hermes.jorisjonkers.dev/auth/callback",
                "https://hermes.jorisjonkers.test/auth/callback",
            )
    }
}
