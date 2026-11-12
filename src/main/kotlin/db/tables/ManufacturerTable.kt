package org.ouraircraft.db.tables

import org.jetbrains.exposed.v1.core.booleanLiteral
import org.jetbrains.exposed.v1.core.dao.id.LongIdTable

/**
 * Stores approved aircraft manufacturers.
 *
 * Contains only the current accepted manufacturer data.
 * Changes are made through [ManufacturerProposalTable].
 */
object ManufacturerTable : LongIdTable("MANUFACTURER") {
    val name = text("name", collate = "NOCASE").index()
    val longName = text("long_name").nullable()
    val deleted = bool("deleted")
}