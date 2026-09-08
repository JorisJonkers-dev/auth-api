package com.jorisjonkers.personalstack.auth.domain.model

/**
 * Represents a named service in the personal stack that requires explicit access grants.
 * Each entry declares the subdomain prefixes that map to it. Multiple subdomains are
 * supported to handle alternative hostnames for the same service.
 *
 * ADMIN users bypass all service permission checks.
 * USER/READONLY users require explicit grants stored in user_service_permissions.
 */
enum class ServicePermission(
    vararg subdomains: String,
) {
    VAULT("vault"),
    MAIL("stalwart"),
    N8N("n8n"),
    GRAFANA("grafana"),

    // Replaces the legacy NOMAD entry; the Headlamp kubernetes dashboard now lives at
    // dashboard.jorisjonkers.dev. Existing rows in user_service_permissions are moved
    // across in migration V7.
    DASHBOARD("dashboard"),
    TRAEFIK("traefik"),
    RABBITMQ("rabbitmq"),

    // agents-ws is the Enschede terminal-WebSocket host; same access as agents.
    AGENTS("agents", "agents-ws"),
    STATUS("status"),

    // Media stack. Surfaced in the app-ui MyApps grid so grantees can land on their
    // Jellyfin / *arr / qBittorrent URLs without memorising subdomains.
    JELLYFIN("jellyfin"),
    JELLYSEERR("jellyseerr"),
    SONARR("sonarr"),
    RADARR("radarr"),
    BAZARR("bazarr"),
    PROWLARR("prowlarr"),
    QBITTORRENT("qbittorrent"),
    IMMICH("immich"),

    // AdGuard Home DNS filter UI at adguard.jorisjonkers.dev. Previously LAN-only on
    // the old utility host; now publicly exposed behind forward-auth so grantees can
    // manage query logs and blocklists off-LAN.
    ADGUARD("adguard"),

    // WolfManager controls the host-native Wolf game streaming service. Actual
    // Moonlight gameplay traffic connects directly to the GTX node.
    WOLF("wolf"),

    // knowledge-api Kotlin/Spring service at kb.jorisjonkers.dev. Phase 4a
    // ships the actuator-only skeleton; the MCP transport + tools land in
    // stacked follow-ups (4b adds /mcp + bearer-bypass middleware, 4c adds
    // the actual recall/lesson/decision tools).
    KNOWLEDGE_API("kb"),

    // Outline wiki at notes.jorisjonkers.dev. Outline runs its own OIDC flow, so the
    // route is `direct` rather than forward-auth protected — the grant is enforced at
    // the authorization endpoint instead, via DOWNSTREAM_CLIENT_PERMISSIONS, exactly
    // as n8n does. Outline's own require-invites setting is the second gate, covering
    // the case where someone already holds a valid authorization code.
    NOTES("notes"),

    // Hermes Agent's web dashboard at hermes.jorisjonkers.dev. Its routes carry
    // no forward-auth at all -- Hermes runs its own OIDC client, and a
    // middleware in front would intercept /auth/callback -- so the grant is
    // enforced at the authorization endpoint via DOWNSTREAM_CLIENT_PERMISSIONS,
    // exactly as NOTES is. Grant sparingly: the dashboard is a control plane
    // for an agent that executes shell commands.
    //
    // The previous comment here said the entry was "required or every request
    // is denied". That is backwards: fromHost returns null for an unlisted
    // subdomain and verify() skips the check entirely, so a missing entry
    // ALLOWS every authenticated user rather than denying them. Which is
    // exactly how overleaf shipped, and why it is being added below.
    HERMES("hermes"),

    // Overleaf Community Edition at overleaf.jorisjonkers.dev. Unlike NOTES and
    // HERMES, this one really is enforced here: Community Edition has no SSO of
    // its own -- SAML, LDAP and OIDC are all Server Pro -- so the route carries
    // the forward-auth middleware and this entry is the only per-user gate in
    // front of it.
    //
    // Without this entry the host resolved to null and every authenticated user
    // reached Overleaf, which is how it shipped in fleet-infra#216. Adding it
    // revokes that access from everyone at once: existing users hold no
    // SERVICE_OVERLEAF row, and only ROLE_ADMIN bypasses the check. Grant
    // before anyone relies on it.
    OVERLEAF("overleaf"),
    ;

    val subdomains: Set<String> = subdomains.toSet()

    companion object {
        private val subdomainIndex: Map<String, ServicePermission> =
            entries
                .flatMap { permission -> permission.subdomains.map { it to permission } }
                .toMap()

        /**
         * Resolves a [ServicePermission] from a hostname such as "vault.jorisjonkers.dev",
         * "dashboard.jorisjonkers.dev", or "stalwart.jorisjonkers.test". Returns null when the
         * host is blank or unrecognised.
         */
        fun fromHost(host: String?): ServicePermission? {
            if (host.isNullOrBlank()) return null
            val subdomain = host.substringBefore(".").lowercase()
            return subdomainIndex[subdomain]
        }
    }
}
