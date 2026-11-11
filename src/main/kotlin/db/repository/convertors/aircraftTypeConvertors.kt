package org.ouraircraft.db.repository.convertors

import org.jetbrains.exposed.v1.core.ResultRow
import org.ouraircraft.db.tables.AircraftTypeTable
import org.ouraircraft.model.data.AircraftType

/**
 * Maps a database row to an [AircraftType], including its manufacturer.
 */
fun ResultRow.toAircraftType(): AircraftType = AircraftType(
    id = this[AircraftTypeTable.id].value,
    manufacturer = toManufacturer(),
    family = this[AircraftTypeTable.family],
    variant = this[AircraftTypeTable.variant],
    icaoDesignator = this[AircraftTypeTable.icaoDesignator],
    engineCount = this[AircraftTypeTable.engineCount],
    engineType = this[AircraftTypeTable.engineType],
    wakeCategory = this[AircraftTypeTable.wakeCategory],
)