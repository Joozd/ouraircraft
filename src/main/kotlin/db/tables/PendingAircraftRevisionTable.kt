package org.ouraircraft.db

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

/** Based-on identity and freshness must be validated in the approval transaction. */
object PendingAircraftRevisionTable : Table("PENDING_AIRCRAFT_REVISION") {
    val id = long("id").autoIncrement()
    val aircraftId = reference("aircraft_id", AircraftTable.id).index()
    val aircraftTypeId = reference("aircraft_type_id", AircraftTypeTable.id)
    val basedOnRevisionId = reference("based_on_revision_id", AircraftRevisionTable.id).nullable()
    val authorId = reference("author_id", UserTable.id).index()
    // Supply normalized registration: uppercase ASCII letters and digits only.
    val registration = text("registration")
    val deleted = bool("deleted")
    val comment = text("comment").default("")
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}
