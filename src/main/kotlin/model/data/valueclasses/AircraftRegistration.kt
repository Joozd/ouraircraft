package org.ouraircraft.model.data.valueclasses

import org.ouraircraft.utils.normalizeRegistration

@JvmInline
value class AircraftRegistration(val value: String){
    init {
        require(value.isNotEmpty() &&
                value.all { it in '0'..'9' || it in 'A'..'Z' }) {
            "Aircraft registration must be non-empty and contain only uppercase letters and digits"
        }
    }
    companion object{
        /**
         * This factory guarantees a valid AircraftRegistration by normalizing the input string to uppercase letters and digits only.
         *
         * Normalizing is done by discarding all characters that are not letters or digits and changing all letters to uppercase.
         * @see normalizeRegistration
         *
         * @throws IllegalArgumentException through regular constructor if the resulting string is empty.
         */
        fun normalizeFromString(value: String): AircraftRegistration = AircraftRegistration(value.normalizeRegistration())
    }
}