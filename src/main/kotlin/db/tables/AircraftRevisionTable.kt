package org.ouraircraft.db

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

/** Full accepted snapshots. Insert only at approval; never update or delete. */
object AircraftRevisionTable : Table("AIRCRAFT_REVISION") {
    val id = long("id").autoIncrement()
    val aircraftId = reference("aircraft_id", AircraftTable.id)
    val aircraftTypeId = reference("aircraft_type_id", AircraftTypeTable.id).index()
    val authorId = reference("author_id", UserTable.id).index()
    val approvedBy = reference("approved_by", UserTable.id)
    // Supply normalized registration: uppercase ASCII letters and digits only.
    val registration = text("registration").index()
    val deleted = bool("deleted")
    val comment = text("comment").default("")
    val createdAt = timestamp("created_at")
    val approvedAt = timestamp("approved_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, aircraftId, id)
    }
}
