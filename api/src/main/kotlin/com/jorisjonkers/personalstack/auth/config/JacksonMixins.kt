package com.jorisjonkers.personalstack.auth.config

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.jorisjonkers.personalstack.auth.domain.model.UserId
import java.util.UUID

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
)
@JsonIgnoreProperties(ignoreUnknown = true)
// Jackson mixin: constructor parameters are consumed by Jackson via @JsonCreator
// during deserialization and are intentionally not referenced in the class body.
class AuthenticatedUserMixin
    @JsonCreator
    constructor(
        @param:JsonProperty("userId") val userId: UserId,
        @param:JsonProperty("username") val username: String,
        @param:JsonProperty("roles") val roles: List<String>,
        @param:JsonProperty("passwordHash") val passwordHash: String,
    )

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
// Jackson mixin: constructor parameter consumed by Jackson via @JsonCreator.
class UserIdMixin
    @JsonCreator
    constructor(
        @param:JsonProperty("value") val value: UUID,
    )
