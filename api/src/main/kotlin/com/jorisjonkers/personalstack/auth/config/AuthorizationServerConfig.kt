package com.jorisjonkers.personalstack.auth.config

import com.jorisjonkers.personalstack.auth.domain.model.ServicePermission
import com.jorisjonkers.personalstack.auth.domain.model.UserId
import com.jorisjonkers.personalstack.auth.infrastructure.security.AuthenticatedUser
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcOperations
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.jackson.SecurityJacksonModules
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.jackson.OAuth2AuthorizationServerJacksonModule
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.context.SecurityContextHolderFilter
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.filter.OncePerRequestFilter

private const val OAUTH2_AUTHORIZE_PATH = "/api/oauth2/authorize"

private fun currentAuthentication(): Authentication? =
    SecurityContextHolder
        .getContext()
        .authentication

private fun isAnonymousOrUnauthenticated(authentication: Authentication?): Boolean =
    authentication == null ||
        authentication is AnonymousAuthenticationToken ||
        !authentication.isAuthenticated

private fun hasClientAccess(
    authentication: Authentication?,
    requiredPermission: ServicePermission,
): Boolean {
    if (authentication == null) return false
    val authorities = authentication.authorities.map { it.authority }.toSet()
    return "ROLE_ADMIN" in authorities || "SERVICE_${requiredPermission.name}" in authorities
}

@Configuration
class AuthorizationServerConfig(
    @param:Value("\${auth.issuer:https://auth.jorisjonkers.dev}")
    private val issuer: String,
    @param:Value("\${auth.login-url:http://localhost:5174/login}")
    private val loginUrl: String,
    @param:Value("\${auth.clients.grafana.secret:grafana-secret}")
    private val grafanaClientSecret: String,
    @param:Value("\${auth.clients.n8n.secret:n8n-secret}")
    private val n8nClientSecret: String,
    @param:Value("\${auth.clients.outline.secret:outline-secret}")
    private val outlineClientSecret: String,
    @param:Value("\${auth.clients.vault.secret:vault-secret}")
    private val vaultClientSecret: String,
) {
    @Bean
    @Order(1)
    fun authorizationServerSecurityFilterChain(
        http: HttpSecurity,
        corsConfigurationSource: CorsConfigurationSource,
        jwkSource: JWKSource<SecurityContext>,
    ): SecurityFilterChain {
        // Hand the JWK source over explicitly.
        //
        // OAuth2AuthorizationServerConfigurer registers NimbusJwkSetEndpointFilter
        // only when it can resolve a JWKSource, and it looks first at this shared
        // object before falling back to a bean lookup by
        // ResolvableType.forClassWithGenerics(JWKSource::class, SecurityContext::class).
        // That fallback does not match the Kotlin `JWKSource<SecurityContext>` bean
        // in JwtConfig, so the filter was never registered and /api/oauth2/jwks
        // reached the DispatcherServlet instead:
        //
        //   NoResourceFoundException: No static resource api/oauth2/jwks
        //
        // Every other endpoint on this chain was fine -- /api/oauth2/authorize
        // answers on its customised path -- which is what made the JWK Set look
        // like a routing problem rather than a missing filter.
        http.setSharedObject(JWKSource::class.java, jwkSource)

        val authServerConfigurer = OAuth2AuthorizationServerConfigurer()
        authServerConfigurer.oidc(Customizer.withDefaults())

        val oauthEndpoints =
            OrRequestMatcher(
                PathPatternRequestMatcher.pathPattern("/api/oauth2/**"),
                PathPatternRequestMatcher.pathPattern("/api/userinfo"),
                PathPatternRequestMatcher.pathPattern("/api/connect/logout"),
                PathPatternRequestMatcher.pathPattern("/.well-known/**"),
            )

        http
            .securityMatcher(oauthEndpoints)
            .cors { it.configurationSource(corsConfigurationSource) }
            .with(authServerConfigurer, Customizer.withDefaults())
            .authorizeHttpRequests { authorize ->
                // The JWK Set endpoint publishes public signing keys and must be
                // readable without authentication: every relying party fetches it
                // to verify an id_token signature, and none of them has a session.
                //
                // Without this, anyRequest().authenticated() gated it. Vault's OIDC
                // login exchanged its code successfully and then failed verifying
                // the id_token, because fetching the keys returned the auth-ui SPA:
                //
                //   failed to verify signature: fetching keys oidc: failed to
                //   decode keys: expected Content-Type = application/json, got
                //   "text/html" ... invalid character '<'
                //
                // The HTML was /login. With an Accept: application/json request the
                // same endpoint answered 401, which is the honest shape of the bug.
                //
                // The sibling discovery document was never affected -- its filter
                // short-circuits before authorization -- so this looked like a
                // Cloudflare or routing fault long before it looked like an
                // authorization rule.
                PUBLIC_OAUTH2_ENDPOINTS.forEach { endpoint ->
                    authorize.requestMatchers(PathPatternRequestMatcher.pathPattern(endpoint)).permitAll()
                }
                authorize.anyRequest().authenticated()
            }.addFilterAfter(downstreamClientAuthorizationFilter(), SecurityContextHolderFilter::class.java)
            .securityContext { ctx ->
                ctx.securityContextRepository(HttpSessionSecurityContextRepository())
            }.exceptionHandling { exceptions ->
                exceptions.defaultAuthenticationEntryPointFor(
                    LoginUrlAuthenticationEntryPoint(loginUrl),
                    MediaTypeRequestMatcher(MediaType.TEXT_HTML),
                )
            }

        return http.build()
    }

    @Bean
    fun registeredClientRepository(): RegisteredClientRepository =
        InMemoryRegisteredClientRepository(
            buildAuthUiClient(),
            buildAppUiClient(),
            buildAppNativeClient(),
            buildAgentsApiClient(),
            buildGrafanaClient(grafanaClientSecret),
            buildN8nClient(n8nClientSecret),
            buildOutlineClient(outlineClientSecret),
            buildRabbitMqClient(),
            buildVaultClient(vaultClientSecret),
            buildHeadlampClient(),
            buildImmichClient(),
            buildHermesClient(),
        )

    // The JdbcOAuth2AuthorizationService constructor calls getColumnMetadata()
    // on the JDBC connection, which means bean creation eagerly opens a real
    // Postgres connection. That breaks the AppCDS training run in the Docker
    // build (no Postgres reachable), so we exclude this bean under the
    // `cds-training` profile and let Spring Security fall back to its
    // in-memory default; the runtime profile (prod) still wires this in.
    @Bean
    @Profile("!cds-training")
    fun authorizationService(
        jdbcOperations: JdbcOperations,
        registeredClientRepository: RegisteredClientRepository,
    ): OAuth2AuthorizationService {
        val jsonMapper = buildAuthorizationJsonMapper()
        val rowMapper =
            JdbcOAuth2AuthorizationService.JsonMapperOAuth2AuthorizationRowMapper(
                registeredClientRepository,
                jsonMapper,
            )
        return JdbcOAuth2AuthorizationService(jdbcOperations, registeredClientRepository).apply {
            setAuthorizationRowMapper(rowMapper)
        }
    }

    private fun buildAuthorizationJsonMapper(): tools.jackson.databind.json.JsonMapper {
        val classLoader = AuthorizationServerConfig::class.java.classLoader
        val typeValidator =
            tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator
                .builder()
                .allowIfSubType("com.jorisjonkers.personalstack.")
                .allowIfSubType("kotlin.")
        return tools.jackson.databind.json.JsonMapper
            .builder()
            .addModules(SecurityJacksonModules.getModules(classLoader, typeValidator))
            .addModule(OAuth2AuthorizationServerJacksonModule())
            .addModule(
                tools.jackson.module.kotlin.KotlinModule
                    .Builder()
                    .build(),
            ).addMixIn(AuthenticatedUser::class.java, AuthenticatedUserMixin::class.java)
            .addMixIn(UserId::class.java, UserIdMixin::class.java)
            .build()
    }

    // Same JDBC-eager-on-construction issue as authorizationService — skip
    // during AppCDS training, fall back to Spring Security's in-memory default.
    @Bean
    @Profile("!cds-training")
    fun authorizationConsentService(
        jdbcOperations: JdbcOperations,
        registeredClientRepository: RegisteredClientRepository,
    ): OAuth2AuthorizationConsentService =
        JdbcOAuth2AuthorizationConsentService(
            jdbcOperations,
            registeredClientRepository,
        )

    @Bean
    fun authorizationServerSettings(): AuthorizationServerSettings =
        AuthorizationServerSettings
            .builder()
            .issuer(issuer)
            .authorizationEndpoint("/api/oauth2/authorize")
            .tokenEndpoint("/api/oauth2/token")
            .jwkSetEndpoint(JWK_SET_ENDPOINT)
            .tokenRevocationEndpoint("/api/oauth2/revoke")
            .tokenIntrospectionEndpoint("/api/oauth2/introspect")
            .oidcUserInfoEndpoint("/api/userinfo")
            .build()

    private fun downstreamClientAuthorizationFilter(): OncePerRequestFilter =
        object : OncePerRequestFilter() {
            override fun shouldNotFilter(request: HttpServletRequest): Boolean {
                val isAuthorizeRequest = request.requestURI == OAUTH2_AUTHORIZE_PATH
                return !isAuthorizeRequest
            }

            override fun doFilterInternal(
                request: HttpServletRequest,
                response: HttpServletResponse,
                filterChain: FilterChain,
            ) {
                val requiredPermission =
                    DOWNSTREAM_CLIENT_PERMISSIONS[request.getParameter("client_id")] ?: run {
                        filterChain.doFilter(request, response)
                        return
                    }
                val authentication = currentAuthentication()

                when {
                    isAnonymousOrUnauthenticated(authentication) -> {
                        filterChain.doFilter(request, response)
                    }

                    !hasClientAccess(authentication, requiredPermission) -> {
                        response.sendError(
                            HttpServletResponse.SC_FORBIDDEN,
                            "Access denied for OAuth client",
                        )
                    }

                    else -> {
                        filterChain.doFilter(request, response)
                    }
                }
            }
        }

    companion object {
        /**
         * Endpoints inside the authorization-server filter chain that must answer
         * without authentication. Kept as a list rather than inlined so
         * [AuthorizationServerConfigTest] can assert it still covers the JWK Set
         * endpoint this server actually advertises in its discovery document.
         */
        const val JWK_SET_ENDPOINT = "/api/oauth2/jwks"

        val PUBLIC_OAUTH2_ENDPOINTS = listOf(JWK_SET_ENDPOINT)

        private val DOWNSTREAM_CLIENT_PERMISSIONS: Map<String, ServicePermission> =
            mapOf(
                "grafana" to ServicePermission.GRAFANA,
                "vault" to ServicePermission.VAULT,
                "n8n" to ServicePermission.N8N,
                "outline" to ServicePermission.NOTES,
                "headlamp" to ServicePermission.DASHBOARD,
                "rabbitmq" to ServicePermission.RABBITMQ,
                "immich" to ServicePermission.IMMICH,
                // Hermes runs its own OIDC flow, so its route carries no
                // forward-auth and the HERMES grant is enforced here at the
                // authorize endpoint instead — the same shape as outline.
                "hermes" to ServicePermission.HERMES,
            )
    }
}
