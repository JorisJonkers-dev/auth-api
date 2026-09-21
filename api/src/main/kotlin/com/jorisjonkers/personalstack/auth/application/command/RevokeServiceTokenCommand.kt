package com.jorisjonkers.personalstack.auth.application.command

import com.jorisjonkers.personalstack.auth.domain.model.UserId
import com.jorisjonkers.personalstack.common.command.Command
import java.util.UUID

data class RevokeServiceTokenCommand(
    val userId: UserId,
    val tokenId: UUID,
) : Command
