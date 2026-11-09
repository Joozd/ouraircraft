package org.ouraircraft.db

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

/** Based-on identity and freshness must be validated in the approval transaction. */
object PendingTypeRevisionTable : Table("PENDING_TYPE_REVISION") {
    val id = long("id").autoIncrement()
    val aircraftTypeId = reference("aircraft_type_id", AircraftTypeTable.id).index()
    val basedOnRevisionId = reference("based_on_revision_id", TypeRevisionTable.id).nullable()
    val authorId = reference("author_id", UserTable.id).index()
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

    override val primaryKey = PrimaryKey(id)
}
