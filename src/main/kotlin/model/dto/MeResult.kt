package org.ouraircraft.model.dto

import kotlinx.serialization.Serializable
import org.ouraircraft.model.dto.sessions.UserSessionData

@Serializable
data class MeResult(
    val user: DtoUser,
    val sessionData: UserSessionData
)
