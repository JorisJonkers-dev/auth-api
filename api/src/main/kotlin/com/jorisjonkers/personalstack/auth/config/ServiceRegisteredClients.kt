package com.jorisjonkers.personalstack.auth.config

import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.oidc.OidcScopes
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import java.time.Duration

private val AGENTS_ACCESS_TOKEN_TTL: Duration = Duration.ofMinutes(15)

fun buildAgentsApiClient(): RegisteredClient =
    RegisteredClient
        .withId(deterministicId("agents-api"))
        .clientId("agents-api")
        .clientSecret("{noop}agents-secret")
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
        .scope("api.read")
        .scope("api.write")
        .tokenSettings(
            TokenSettings
                .builder()
                .accessTokenTimeToLive(AGENTS_ACCESS_TOKEN_TTL)
                .build(),
        ).build()

fun buildGrafanaClient(secret: String): RegisteredClient =
    RegisteredClient
        .withId(deterministicId("grafana"))
        .clientId("grafana")
        .clientSecret("{noop}$secret")
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .redirectUri("https://grafana.jorisjonkers.dev/login/generic_oauth")
        .redirectUri("https://grafana.jorisjonkers.test/login/generic_oauth")
        .scope(OidcScopes.OPENID)
        .scope(OidcScopes.PROFILE)
        .scope(OidcScopes.EMAIL)
        .clientSettings(noConsentSettings(requirePkce = false))
        .tokenSettings(defaultTokenSettings())
        .build()

fun buildN8nClient(secret: String): RegisteredClient =
    RegisteredClient
        .withId(deterministicId("n8n"))
        .clientId("n8n")
        .clientSecret("{noop}$secret")
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .redirectUri("https://n8n.jorisjonkers.dev/auth/oidc/callback")
        .redirectUri("https://n8n.jorisjonkers.test/auth/oidc/callback")
        .scope(OidcScopes.OPENID)
        .scope(OidcScopes.PROFILE)
        .scope(OidcScopes.EMAIL)
        .clientSettings(noConsentSettings(requirePkce = false))
        .tokenSettings(defaultTokenSettings())
        .build()

fun buildRabbitMqClient(): RegisteredClient =
    RegisteredClient
        .withId(deterministicId("rabbitmq"))
        .clientId("rabbitmq")
        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .redirectUri("https://rabbitmq.jorisjonkers.dev/js/oidc-oauth/login-callback.html")
        .redirectUri("https://rabbitmq.jorisjonkers.test/js/oidc-oauth/login-callback.html")
        .scope(OidcScopes.OPENID)
        .scope(OidcScopes.PROFILE)
        .scope(OidcScopes.EMAIL)
        .clientSettings(noConsentSettings(requirePkce = true))
        .tokenSettings(defaultTokenSettings())
        .build()

fun buildHeadlampClient(): RegisteredClient =
    RegisteredClient
        .withId(deterministicId("headlamp"))
        .clientId("headlamp")
        // Public client with PKCE — same pattern as rabbitmq. There is no
        // client secret to manage anywhere (no Vault key, no K8s secret),
        // which removes the one-time-bootstrap step from the OIDC flow.
        // The Headlamp backend proves possession of the auth code via
        // the PKCE verifier instead.
        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .redirectUri("https://dashboard.jorisjonkers.dev/oidc-callback")
        .redirectUri("https://dashboard.jorisjonkers.test/oidc-callback")
        .scope(OidcScopes.OPENID)
        .scope(OidcScopes.PROFILE)
        .scope(OidcScopes.EMAIL)
        // `groups` is a non-standard scope the token customizer recognises
        // and populates with the Kubernetes group membership the k3s API
        // server reads via --oidc-groups-claim. The chain is: DASHBOARD
        // ServicePermission -> `k8s-admin` in the groups claim ->
        // cluster-admin via the oidc:k8s-admin ClusterRoleBinding.
        .scope("groups")
        .clientSettings(noConsentSettings(requirePkce = true))
        .tokenSettings(defaultTokenSettings())
        .build()

fun buildHermesClient(): RegisteredClient =
    RegisteredClient
        .withId(deterministicId("hermes"))
        .clientId("hermes")
        // Public client with PKCE — same pattern as headlamp and rabbitmq.
        // Hermes' dashboard takes only HERMES_DASHBOARD_OIDC_ISSUER,
        // _CLIENT_ID and _SCOPES; it has no field for a client secret, so a
        // confidential client could not be configured even if we wanted one.
        // The dashboard proves possession of the auth code with the PKCE
        // verifier instead, and there is no secret to rotate in Vault.
        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        // Hermes builds the callback as <public_url>/auth/callback verbatim,
        // where public_url is HERMES_DASHBOARD_PUBLIC_URL. These must match
        // that construction exactly or the flow fails at the redirect.
        .redirectUri("https://hermes.jorisjonkers.dev/auth/callback")
        .redirectUri("https://hermes.jorisjonkers.test/auth/callback")
        .scope(OidcScopes.OPENID)
        .scope(OidcScopes.PROFILE)
        .scope(OidcScopes.EMAIL)
        .clientSettings(noConsentSettings(requirePkce = true))
        .tokenSettings(defaultTokenSettings())
        .build()

fun buildImmichClient(): RegisteredClient =
    RegisteredClient
        .withId(deterministicId("immich"))
        .clientId("immich")
        .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .redirectUri("https://immich.jorisjonkers.dev/auth/login")
        .redirectUri("https://immich.jorisjonkers.test/auth/login")
        .redirectUri("app.immich:///oauth-callback")
        .scope(OidcScopes.OPENID)
        .scope(OidcScopes.PROFILE)
        .scope(OidcScopes.EMAIL)
        .clientSettings(noConsentSettings(requirePkce = true))
        .tokenSettings(defaultTokenSettings())
        .build()

// Outline wiki at notes.jorisjonkers.dev. Outline drives the whole OIDC dance itself
// rather than sitting behind forward-auth, so the callback path is Outline's own
// `/auth/oidc.callback` — note the dot, not a slash; a slash silently yields
// redirect_uri_mismatch. Outline does not use the discovery document, so the auth,
// token and userinfo URIs are configured explicitly on the Outline side.
fun buildOutlineClient(secret: String): RegisteredClient =
    RegisteredClient
        .withId(deterministicId("outline"))
        .clientId("outline")
        .clientSecret("{noop}$secret")
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .redirectUri("https://notes.jorisjonkers.dev/auth/oidc.callback")
        .redirectUri("https://notes.jorisjonkers.test/auth/oidc.callback")
        .scope(OidcScopes.OPENID)
        .scope(OidcScopes.PROFILE)
        .scope(OidcScopes.EMAIL)
        .clientSettings(noConsentSettings(requirePkce = false))
        .tokenSettings(defaultTokenSettings())
        .build()

fun buildVaultClient(secret: String): RegisteredClient =
    RegisteredClient
        .withId(deterministicId("vault"))
        .clientId("vault")
        .clientSecret("{noop}$secret")
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
        .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
        .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
        .redirectUri("https://vault.jorisjonkers.dev/ui/vault/auth/oidc/oidc/callback")
        .redirectUri("https://vault.jorisjonkers.test/ui/vault/auth/oidc/oidc/callback")
        .redirectUri("http://localhost:8250/oidc/callback")
        .redirectUri("http://127.0.0.1:8250/oidc/callback")
        .scope(OidcScopes.OPENID)
        .scope(OidcScopes.PROFILE)
        .scope(OidcScopes.EMAIL)
        .clientSettings(noConsentSettings(requirePkce = false))
        .tokenSettings(defaultTokenSettings())
        .build()
