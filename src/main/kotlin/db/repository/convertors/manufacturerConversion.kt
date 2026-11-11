package org.ouraircraft.db.repository.convertors

import org.jetbrains.exposed.v1.core.ResultRow
import org.ouraircraft.db.tables.ManufacturerTable
import org.ouraircraft.model.data.Manufacturer

/**
 * Maps a database row to a [Manufacturer].
 */
fun ResultRow.toManufacturer(): Manufacturer = Manufacturer(
    id = this[ManufacturerTable.id].value,
    name = this[ManufacturerTable.name],
    longName = this[ManufacturerTable.longName],
)