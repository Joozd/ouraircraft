package org.ouraircraft.model.dto.data

import kotlinx.serialization.Serializable

/**
 * Represents an aircraft manufacturer.
 *
 * @property id Unique identifier for the manufacturer, stable on OurAircraft.
 * @property name Short or commonly used manufacturer name.
 * @property longName Full manufacturer name.
 */
@Serializable
data class ManufacturerDto(
    val id: Long,
    val name: String,
    val longName: String?
)
