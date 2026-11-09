package org.ouraircraft.db

import org.jetbrains.exposed.v1.core.Table

object AircraftTypeTable : Table("AIRCRAFT_TYPE") {
    val id = long("id").autoIncrement()

    override val primaryKey = PrimaryKey(id)
}
