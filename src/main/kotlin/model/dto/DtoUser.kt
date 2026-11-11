package org.ouraircraft.model.dto

import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class DtoUser(
    val id: Uuid,
    val screenName: String,
    val createdAt: Instant
)