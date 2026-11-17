package org.ouraircraft.model.dto.data

import kotlinx.serialization.Serializable

/**
 * Represents an individual aircraft.
 *
 * @property id Unique identifier for the aircraft, stable on OurAircraft.
 * @property type Aircraft type associated with this aircraft.
 * @property registration Aircraft registration in its normalized form.
 */
@Serializable
data class AircraftDto(
    val id: Long,
    val type: AircraftTypeDto,
    val registration: String
)
