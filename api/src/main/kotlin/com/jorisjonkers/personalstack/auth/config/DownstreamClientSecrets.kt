package com.jorisjonkers.personalstack.auth.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class DownstreamClientSecrets(
    @param:Value("\${auth.clients.grafana.secret:grafana-secret}")
    val grafana: String,
    @param:Value("\${auth.clients.n8n.secret:n8n-secret}")
    val n8n: String,
    @param:Value("\${auth.clients.outline.secret:outline-secret}")
    val outline: String,
    @param:Value("\${auth.clients.vault.secret:vault-secret}")
    val vault: String,
    @param:Value("\${auth.clients.tribelt.secret:tribelt-secret}")
    val tribelt: String,
)
