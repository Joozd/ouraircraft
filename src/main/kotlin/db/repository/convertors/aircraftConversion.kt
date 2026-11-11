package org.ouraircraft.db.repository.convertors

import org.jetbrains.exposed.v1.core.ResultRow
import org.ouraircraft.db.tables.AircraftTable
import org.ouraircraft.model.data.Aircraft

/**
 * Maps a database row to an [Aircraft], including its type and manufacturer.
 */
fun ResultRow.toAircraft(): Aircraft = Aircraft(
    id = this[AircraftTable.id].value,
    type = toAircraftType(),
    registration = this[AircraftTable.registration],
)