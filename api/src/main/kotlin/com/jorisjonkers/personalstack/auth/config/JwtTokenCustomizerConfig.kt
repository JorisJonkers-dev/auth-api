package com.jorisjonkers.personalstack.auth.config

import com.jorisjonkers.personalstack.auth.domain.model.Role
import com.jorisjonkers.personalstack.auth.domain.model.ServicePermission
import com.jorisjonkers.personalstack.auth.domain.model.UserCredentials
import com.jorisjonkers.personalstack.auth.domain.port.UserRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.oidc.OidcScopes
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer

@Configuration
class JwtTokenCustomizerConfig {
    @Bean
    fun jwtTokenCustomizer(userRepository: UserRepository): OAuth2TokenCustomizer<JwtEncodingContext> =
        OAuth2TokenCustomizer { context ->
            val principal = context.getPrincipal<Authentication>()
            val credentials =
                userRepository.findCredentialsByUsername(principal.name) ?: return@OAuth2TokenCustomizer
            val roles = buildRoles(credentials)

            when {
                context.tokenType == OAuth2TokenType.ACCESS_TOKEN ->
                    customizeAccessToken(context, credentials, roles)
                context.tokenType.value == OidcParameterNames.ID_TOKEN ->
                    customizeIdToken(context, credentials, roles)
            }
        }

    private fun customizeAccessToken(
        context: JwtEncodingContext,
        credentials: UserCredentials,
        roles: List<String>,
    ) {
        context.claims.claim("roles", roles)
        context.claims.claim("username", credentials.username)
        context.claims.claim("preferred_username", credentials.username)
        context.claims.claim("email", credentials.email)
        context.claims.claim("aud", listOf(context.registeredClient.clientId))
        context.claims.subject(credentials.userId.value.toString())
    }

    private fun customizeIdToken(
        context: JwtEncodingContext,
        credentials: UserCredentials,
        roles: List<String>,
    ) {
        context.claims.claim("roles", roles)
        context.claims.subject(credentials.userId.value.toString())

        if (OidcScopes.PROFILE in context.authorizedScopes) {
            context.claims.claim("preferred_username", credentials.username)
            context.claims.claim("name", "${credentials.firstName} ${credentials.lastName}")
        }

        if (OidcScopes.EMAIL in context.authorizedScopes) {
            context.claims.claim("email", credentials.email)
            context.claims.claim("email_verified", credentials.emailConfirmed)
        }

        val k8sGroups = kubernetesGroups(credentials)
        if (k8sGroups.isNotEmpty() && "groups" in context.authorizedScopes) {
            context.claims.claim("groups", k8sGroups)
        }
    }

    private fun buildRoles(credentials: UserCredentials): List<String> =
        buildList {
            add("ROLE_${credentials.role.name}")
            if (credentials.role == Role.ADMIN) {
                addAll(ServicePermission.entries.map { "SERVICE_${it.name}" })
            } else {
                addAll(credentials.servicePermissions.map { "SERVICE_${it.name}" })
            }
        }

    // Kubernetes group membership for ID tokens issued to OIDC clients that
    // talk to the k3s API server (Headlamp). The k3s control plane runs
    // --oidc-groups-claim=groups --oidc-groups-prefix=oidc: and we bind
    // oidc:k8s-admin to the cluster-admin ClusterRole. DASHBOARD permission
    // therefore grants full cluster management, matching how the host-level
    // forward-auth already gates access to dashboard.jorisjonkers.dev.
    private fun kubernetesGroups(credentials: UserCredentials): List<String> =
        if (credentials.role == Role.ADMIN ||
            ServicePermission.DASHBOARD in credentials.servicePermissions
        ) {
            listOf("k8s-admin")
        } else {
            emptyList()
        }
}
