package org.ouraircraft.model.dto

import kotlinx.serialization.Serializable

/**
 * Represents a structured API error response.
 *
 * [code] is a stable machine-readable identifier.
 * [message] provides a human-readable description.
 */
@Serializable
data class ApiError(
    val code: String,
    val message: String
)