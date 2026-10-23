package com.jorisjonkers.personalstack.auth.config

import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.oidc.OidcScopes
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient

// The Findability Explorer at digital-strategy.jorisjonkers.dev: a confidential client that also
// sends PKCE, the tribelt shape. It keeps the refresh token to re-read the roles claim, where
// ROLE_ADMIN is its admin role; the FINDABILITY_EXPLORER grant is enforced at the authorize endpoint.
fun buildFindabilityExplorerClient(secret: String): RegisteredClient =
    RegisteredClient
        .withId(deterministicId("findability-explorer"))
        .clientId("findability-explorer")
        .clientSecret("{noop}$secret")
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .redirectUri("https://digital-strategy.jorisjonkers.dev/auth/callback")
        .redirectUri("https://digital-strategy.jorisjonkers.test/auth/callback")
        .postLogoutRedirectUri("https://digital-strategy.jorisjonkers.dev/")
        .postLogoutRedirectUri("https://digital-strategy.jorisjonkers.test/")
        .scope(OidcScopes.OPENID)
        .scope(OidcScopes.PROFILE)
        .scope(OidcScopes.EMAIL)
        .clientSettings(noConsentSettings(requirePkce = true))
        .tokenSettings(defaultTokenSettings())
        .build()
