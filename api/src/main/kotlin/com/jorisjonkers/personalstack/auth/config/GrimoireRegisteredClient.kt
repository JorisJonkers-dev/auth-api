package com.jorisjonkers.personalstack.auth.config

import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.oidc.OidcScopes
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient

// Grimoire at grimoire.jorisjonkers.dev: a confidential client that also sends PKCE, the tribelt
// shape. Grimoire keeps its own Accounts and signs one in from the ID token's subject, so the
// callback is Grimoire's own `/oidc/callback` under its base URL.
fun buildGrimoireClient(secret: String): RegisteredClient =
    RegisteredClient
        .withId(deterministicId("grimoire"))
        .clientId("grimoire")
        .clientSecret("{noop}$secret")
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .redirectUri("https://grimoire.jorisjonkers.dev/oidc/callback")
        .redirectUri("https://grimoire.jorisjonkers.test/oidc/callback")
        .scope(OidcScopes.OPENID)
        .scope(OidcScopes.PROFILE)
        .scope(OidcScopes.EMAIL)
        .clientSettings(noConsentSettings(requirePkce = true))
        .tokenSettings(defaultTokenSettings())
        .build()

// What a Vault template renders for a key that does not exist yet.
private const val UNPROVISIONED_SECRET = "<no value>"

// Grimoire's client has no fallback secret: until its Vault key is provisioned the client is not
// registered at all, rather than registered with a secret anybody could guess.
fun grimoireClientOrNull(secret: String): RegisteredClient? =
    secret.takeIf { it.isNotBlank() && it != UNPROVISIONED_SECRET }?.let(::buildGrimoireClient)
