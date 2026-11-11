package org.ouraircraft.model.data

/**
 * Represents an aircraft type.
 *
 * @property id Unique identifier for the aircraft type, stable on OurAircraft.
 * @property manufacturer Manufacturer of this aircraft type.
 * @property family Aircraft family name.
 * @property variant Specific variant within the aircraft family.
 * @property icaoDesignator ICAO aircraft type designator.
 * @property engineCount Number of engines.
 * @property engineType Type of propulsion or engine.
 * @property wakeCategory ICAO wake turbulence category.
 */
data class AircraftType(
    val id: Long,
    val manufacturer: Manufacturer,
    val family: String,
    val variant: String,
    val icaoDesignator: String,
    val engineCount: Int,
    val engineType: String,
    val wakeCategory: String,
)