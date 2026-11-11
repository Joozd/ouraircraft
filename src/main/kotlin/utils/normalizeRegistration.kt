package org.ouraircraft.utils

import kotlin.text.iterator

/**
 * Keep only digits and letters, and convert to uppercase. This is the same as the database constraint.
 */
fun String.normalizeRegistration(): String = buildString {
    for (c in this@normalizeRegistration) {
        val upper = c.uppercaseChar()
        if (upper in 'A'..'Z' || upper in '0'..'9'){
            append(upper)
        }
    }
}