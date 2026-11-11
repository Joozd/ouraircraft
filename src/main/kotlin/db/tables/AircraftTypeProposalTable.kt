package org.ouraircraft.db.tables

/**
 * Stores proposed creations, modifications, and deletions of aircraft types.
 *
 * Each proposal contains a complete snapshot of the proposed aircraft type data.
 * Proposals also serve as an audit trail: their contents are immutable after creation,
 * except for the (nullable) review fields, which may be populated once when a decision is made.
 *
 * A null [targetAircraftTypeId] indicates a proposal for a new aircraft type.
 * Otherwise, the proposal targets an existing aircraft type.
 *
 * Pending proposals have null review fields. Upon review, [checkedBy],
 * [checkResult], [checkResultComment], and [checkedAt] are finalized.
 * Accepted proposals reference the resulting aircraft type through [resultAircraftTypeId].
 * Rejected proposals leave this reference null.
 *
 * Acceptance and updates to [AircraftTypeTable] must occur in the same transaction
 * to maintain consistency between the audit trail and the accepted records.
 */
object AircraftTypeProposalTable : CheckableTable("TYPE_PROPOSAL") {
    val targetAircraftTypeId = reference("target_aircraft_type_id", AircraftTypeTable.id).nullable()
    val resultAircraftTypeId = reference("result_aircraft_type_id", AircraftTypeTable.id).nullable()
    val authorId = reference("author_id", UserTable.id).index()
    val manufacturerId = reference("manufacturer_id", ManufacturerTable.id)
    val family = text("family")
    val variant = text("variant")
    val icaoDesignator = text("icao_designator")
    val engineCount = integer("engine_count")
    val engineType = text("engine_type")
    val wakeCategory = text("wake_category")
    val delete = bool("delete") // proposed deletion
    val proposalComment = text("proposal_comment").nullable()
}
