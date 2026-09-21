package com.jorisjonkers.personalstack.auth.application.command

import com.jorisjonkers.personalstack.auth.domain.model.ServicePermission
import com.jorisjonkers.personalstack.auth.domain.model.UserId
import com.jorisjonkers.personalstack.common.command.Command

data class MintServiceTokenCommand(
    val userId: UserId,
    val service: ServicePermission,
    val label: String,
) : Command
