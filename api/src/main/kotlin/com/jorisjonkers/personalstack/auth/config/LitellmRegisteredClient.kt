package com.jorisjonkers.personalstack.auth.config

import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.oidc.OidcScopes
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient

// The LiteLLM proxy's admin UI at litellm.jorisjonkers.dev: a confidential client that also sends
// PKCE, the tribelt shape. LiteLLM runs its own generic OIDC flow (callback /sso/callback), reads
// the roles claim of the ID token, and maps ROLE_ADMIN to proxy admin; the LITELLM grant is
// enforced at the authorize endpoint. No refresh token: the UI session is LiteLLM's own.
// Built only through litellmClientOrNull: there is no fallback secret.
fun buildLitellmClient(secret: String): RegisteredClient =
    RegisteredClient
        .withId(deterministicId("litellm"))
        .clientId("litellm")
        .clientSecret("{noop}$secret")
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .redirectUri("https://litellm.jorisjonkers.dev/sso/callback")
        .redirectUri("https://litellm.jorisjonkers.test/sso/callback")
        .scope(OidcScopes.OPENID)
        .scope(OidcScopes.PROFILE)
        .scope(OidcScopes.EMAIL)
        .clientSettings(noConsentSettings(requirePkce = true))
        .tokenSettings(defaultTokenSettings())
        .build()

// No fallback secret, the grimoire shape: until its Vault key is provisioned the client is not
// registered at all, rather than registered with a secret anybody could guess.
fun litellmClientOrNull(secret: String): RegisteredClient? =
    secret.takeIf { it.isNotBlank() && it != UNPROVISIONED_SECRET }?.let(::buildLitellmClient)
