package org.ouraircraft.model.dto

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

@Serializable
data class EmailVerificationData(val id: Uuid, val emailVerificationTokenBase64: String) {
}