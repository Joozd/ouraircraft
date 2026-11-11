package org.ouraircraft.db.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable



/**
 * Stores approved aircraft types.
 *
 * Each aircraft type references an approved manufacturer.
 * Contains only the current accepted aircraft type data.
 * Changes are made through [AircraftTypeProposalTable].
 */
object AircraftTypeTable : LongIdTable("AIRCRAFT_TYPE") {
    @Suppress("unused") // used in join
    val manufacturerId = reference("manufacturer_id", ManufacturerTable.id).index()
    val family = text("family", collate = "NOCASE").index()
    val variant = text("variant")
    val icaoDesignator = text("icao_designator", collate = "NOCASE").index()
    val engineCount = integer("engine_count")
    val engineType = text("engine_type")
    val wakeCategory = text("wake_category")
}