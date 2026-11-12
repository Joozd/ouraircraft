package org.ouraircraft.utils

/**
 * Keep only digits and letters, and convert to uppercase. This is the same as the database constraint.
 */
fun String.normalizeRegistration(): String =
    filter { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' }
        .uppercase()