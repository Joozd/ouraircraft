package org.ouraircraft.model.data

import org.ouraircraft.model.data.valueclasses.AircraftRegistration

/**
 * Represents an individual aircraft.
 *
 * @property id Unique identifier for the aircraft, stable on OurAircraft.
 * @property type Aircraft type associated with this aircraft.
 * @property registration Aircraft registration in its normalized form.
 */
data class Aircraft(
    val id: Long,
    val type: AircraftType,
    val registration: AircraftRegistration
)