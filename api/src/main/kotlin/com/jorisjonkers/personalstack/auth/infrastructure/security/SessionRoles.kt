package com.jorisjonkers.personalstack.auth.infrastructure.security

import com.jorisjonkers.personalstack.auth.domain.model.Role
import com.jorisjonkers.personalstack.auth.domain.model.ServicePermission
import com.jorisjonkers.personalstack.auth.domain.model.UserCredentials

/** The authorities a browser session carries: the role, plus every service for an admin. */
fun sessionRoles(
    role: Role,
    servicePermissions: Set<ServicePermission>,
): List<String> =
    buildList {
        add("ROLE_${role.name}")
        if (role == Role.ADMIN) {
            addAll(ServicePermission.entries.map { "SERVICE_${it.name}" })
        } else {
            addAll(servicePermissions.map { "SERVICE_${it.name}" })
        }
    }

fun UserCredentials.sessionRoles(): List<String> = sessionRoles(role, servicePermissions)
