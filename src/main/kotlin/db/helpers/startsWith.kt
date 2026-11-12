package org.ouraircraft.db.helpers

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.TextColumnType
import org.ouraircraft.db.utils.appendAll
import org.ouraircraft.utils.normalizeRegistration
import org.ouraircraft.db.utils.op
import org.ouraircraft.model.data.valueclasses.AircraftRegistration

private const val MINIMUM_SEARCH_LENGTH = 2

/**
 * Matches aircraft registrations starting with [query].
 * Requires at least two alphanumeric characters after normalization.
 */
@JvmName("registrationStartsWith") // prevent JVM clash after erasing type
infix fun Column<AircraftRegistration>.startsWith(query: String): Op<Boolean> {
    val prefix = query.normalizeRegistration()
    if (prefix.length < MINIMUM_SEARCH_LENGTH) return Op.FALSE

    return op {
        appendAll(this@startsWith, " LIKE ")
        registerArgument(TextColumnType(), "$prefix%")
    }
}

/**
 * Matches strings starting with [query].
 *
 * Requires at least two characters after trimming whitespace.
 * Escapes SQL LIKE wildcards to ensure literal prefix matching.
 *
 * Case sensitivity depends on the database's LIKE behavior.
 */
infix fun Column<String>.startsWith(query: String): Op<Boolean> {
    val prefix = query.trim()
        .also { if (it.length < MINIMUM_SEARCH_LENGTH) return Op.FALSE }
        .replace("!", "!!")
        .replace("%", "!%")
        .replace("_", "!_")

    return op {
        appendAll(this@startsWith, " LIKE ")
        registerArgument(TextColumnType(), "$prefix%")
        append(" ESCAPE '!'")
    }
}