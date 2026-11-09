package org.ouraircraft.db

import org.jetbrains.exposed.v1.core.Table

object AircraftTable : Table("AIRCRAFT") {
    val id = long("id").autoIncrement()

    override val primaryKey = PrimaryKey(id)
}
