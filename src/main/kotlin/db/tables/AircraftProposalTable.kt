package org.ouraircraft.db.tables

import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.datetime.timestamp
import org.ouraircraft.db.helpers.asAircraftRegistration

/**
 * Stores proposed creations, modifications, and deletions of aircraft.
 *
 * Each proposal contains a complete snapshot of the proposed aircraft data.
 * Proposals also serve as an audit trail: their contents are immutable after creation,
 * except for the review fields, which may be populated once when a decision is made.
 *
 * A null [targetAircraftId] indicates a proposal for a new aircraft.
 * Otherwise, the proposal targets an existing aircraft.
 *
 * Pending proposals have null review fields. Upon review, [checkedBy],
 * [checkResult], and [checkedAt] are finalized, while [checkResultComment]
 * may optionally be populated.
 *
 * Accepted proposals reference the resulting aircraft through [resultAircraftId].
 * Rejected proposals leave this reference null.
 *
 * The registration must be normalized to uppercase ASCII letters and digits
 * before insertion.
 *
 * The target aircraft's identity and freshness must be validated during approval.
 * Acceptance and updates to [AircraftTable] must occur in the same transaction
 * to maintain consistency between the audit trail and the accepted records.
 */
object AircraftProposalTable : CheckableTable("AIRCRAFT_PROPOSAL") {
    val targetAircraftId = reference("target_aircraft_id", AircraftTable.id).nullable()
    val resultAircraftId = reference("result_aircraft_id", AircraftTable.id).nullable()
    val authorId = reference("author_id", UserTable.id).index()
    val aircraftTypeId = reference("aircraft_type_id", AircraftTypeTable.id)

    // Supply normalized registration: uppercase ASCII letters and digits only.
    val registration = text("registration").asAircraftRegistration()
    val delete = bool("delete")
    val proposalComment = text("proposal_comment").nullable()
    val createdAt = timestamp("created_at")

    init {
        index(
            customIndexName = "aircraft_proposal_pending_created_at",
            columns = arrayOf(createdAt),
            filterCondition = { checkResult.isNull() }
        )
    }
}
