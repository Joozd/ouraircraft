package org.ouraircraft.db.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.ouraircraft.db.helpers.asAircraftRegistration

/**
 * Stores approved individual aircraft.
 *
 * Each aircraft references an approved aircraft type.
 * Contains only the current accepted aircraft data.
 * Changes are made through [AircraftProposalTable].
 */
object AircraftTable : LongIdTable("AIRCRAFT") {
    val aircraftTypeId = reference("aircraft_type_id", AircraftTypeTable.id).index()
    val registration = text("registration").asAircraftRegistration().index()
    val deleted = bool("deleted")
}