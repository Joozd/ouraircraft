package org.ouraircraft.model.data

/**
 * Represents an aircraft manufacturer.
 *
 * @property id Unique identifier for the manufacturer, stable on OurAircraft.
 * @property name Short or commonly used manufacturer name.
 * @property longName Full manufacturer name.
 */
data class Manufacturer(
    val id: Long,
    val name: String,
    val longName: String?
)
