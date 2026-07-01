package com.jorisjonkers.personalstack.auth.config

import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.oidc.OidcScopes
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import java.time.Duration
import java.util.UUID

private val ACCESS_TOKEN_TTL: Duration = Duration.ofMinutes(15)
private val REFRESH_TOKEN_TTL: Duration = Duration.ofDays(7)

internal fun deterministicId(clientId: String): String = UUID.nameUUIDFromBytes(clientId.toByteArray()).toString()

internal fun defaultTokenSettings(): TokenSettings =
    TokenSettings
        .builder()
        .accessTokenTimeToLive(ACCESS_TOKEN_TTL)
        .refreshTokenTimeToLive(REFRESH_TOKEN_TTL)
        .reuseRefreshTokens(false)
        .build()

internal fun noConsentSettings(requirePkce: Boolean): ClientSettings =
    ClientSettings
        .builder()
        .requireProofKey(requirePkce)
        .requireAuthorizationConsent(false)
        .build()

fun buildAuthUiClient(): RegisteredClient =
    RegisteredClient
        .withId(deterministicId("auth-ui"))
        .clientId("auth-ui")
        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .redirectUri("https://auth.jorisjonkers.dev/callback")
        .redirectUri("https://auth.jorisjonkers.test/callback")
        .redirectUri("http://localhost:5174/callback")
        .postLogoutRedirectUri("https://auth.jorisjonkers.dev/logged-out")
        .postLogoutRedirectUri("https://auth.jorisjonkers.test/logged-out")
        .scope(OidcScopes.OPENID)
        .scope(OidcScopes.PROFILE)
        .scope(OidcScopes.EMAIL)
        .clientSettings(noConsentSettings(requirePkce = true))
        .tokenSettings(defaultTokenSettings())
        .build()

fun buildAppUiClient(): RegisteredClient =
    RegisteredClient
        .withId(deterministicId("app-ui"))
        .clientId("app-ui")
        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .redirectUri("https://jorisjonkers.dev/callback")
        .redirectUri("https://jorisjonkers.test/callback")
        .redirectUri("http://localhost:5175/callback")
        .postLogoutRedirectUri("https://jorisjonkers.dev")
        .postLogoutRedirectUri("https://jorisjonkers.test")
        .postLogoutRedirectUri("http://localhost:5175")
        .scope(OidcScopes.OPENID)
        .scope(OidcScopes.PROFILE)
        .scope(OidcScopes.EMAIL)
        .clientSettings(noConsentSettings(requirePkce = true))
        .tokenSettings(defaultTokenSettings())
        .build()

fun buildAppNativeClient(): RegisteredClient =
    RegisteredClient
        .withId(deterministicId("app-native"))
        .clientId("app-native")
        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        // The native custom scheme must match the Capacitor app id configured in spec 021.
        .redirectUri("app.jorisjonkers://callback")
        .redirectUri("capacitor://localhost/callback")
        .redirectUri("http://localhost/callback")
        .postLogoutRedirectUri("app.jorisjonkers://callback")
        .postLogoutRedirectUri("capacitor://localhost/callback")
        .postLogoutRedirectUri("http://localhost/callback")
        .scope(OidcScopes.OPENID)
        .scope(OidcScopes.PROFILE)
        .scope(OidcScopes.EMAIL)
        .clientSettings(noConsentSettings(requirePkce = true))
        .tokenSettings(defaultTokenSettings())
        .build()
