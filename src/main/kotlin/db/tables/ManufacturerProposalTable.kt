package org.ouraircraft.db.tables

/**
 * Stores proposed creations, modifications, and deletions of manufacturers.
 *
 * Each proposal contains a complete snapshot of the proposed manufacturer data.
 * Proposals also serve as an audit trail: their contents are immutable after creation,
 * except for the review fields, which may be populated once when a decision is made.
 *
 * A null [targetManufacturerId] indicates a proposal for a new manufacturer.
 * Otherwise, the proposal targets an existing manufacturer.
 *
 * Pending proposals have null review fields. Upon review, [checkedBy],
 * [checkResult], and [checkedAt] are finalized, while [checkResultComment]
 * may optionally be populated.
 *
 * Accepted proposals reference the resulting manufacturer through [resultManufacturerId].
 * Rejected proposals leave this reference null.
 *
 * The target manufacturer's identity and freshness must be validated during approval.
 * Acceptance and updates to [ManufacturerTable] must occur in the same transaction
 * to maintain consistency between the audit trail and the accepted records.
 */
object ManufacturerProposalTable: CheckableTable("MANUFACTURER_PROPOSAL") {
    val targetManufacturerId = reference("target_manufacturer_id", ManufacturerTable.id).nullable()
    val resultManufacturerId = reference("result_manufacturer_id", ManufacturerTable.id).nullable()
    val authorId = reference("author_id", UserTable.id).index()
    val name = text("name")
    val longName = text("long_name").nullable()
    val delete = bool("delete")
    val proposalComment = text("proposal_comment").nullable()
}