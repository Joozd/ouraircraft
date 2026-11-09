package org.ouraircraft.db

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

/** Full accepted snapshots. Insert only at approval; never update or delete. */
object TypeRevisionTable : Table("TYPE_REVISION") {
    val id = long("id").autoIncrement()
    val aircraftTypeId = reference("aircraft_type_id", AircraftTypeTable.id)
    val authorId = reference("author_id", UserTable.id).index()
    val approvedBy = reference("approved_by", UserTable.id)
    val manufacturer = text("manufacturer")
    val family = text("family")
    val variant = text("variant")
    val icaoDesignator = text("icao_designator")
    val engineCount = integer("engine_count")
    val engineType = text("engine_type")
    val wakeCategory = text("wake_category")
    val deleted = bool("deleted")
    val comment = text("comment").default("")
    val createdAt = timestamp("created_at")
    val approvedAt = timestamp("approved_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, aircraftTypeId, id)
    }
}
