package org.ouraircraft.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class PasskeyRegistrationResponse(
    val label: String,
    val passkeyRegistrationResponseJson: String
)
