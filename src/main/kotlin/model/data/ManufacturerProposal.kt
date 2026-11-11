package org.ouraircraft.model.data

import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Represents a proposal to create, modify, or delete a manufacturer.
 *
 * Contains a complete snapshot of the proposed manufacturer data.
 * Once created, proposal data is immutable. Review fields may be populated
 * once when the proposal is accepted or rejected.
 *
 * @property id Unique identifier of this proposal.
 * @property targetManufacturerId ID of the manufacturer being modified or deleted,
 *   or null if proposing a new manufacturer.
 * @property resultManufacturerId ID of the resulting manufacturer after acceptance,
 *   or null if pending or rejected.
 * @property authorId ID of the user who submitted the proposal.
 * @property name Proposed manufacturer name.
 * @property longName Optional full manufacturer name.
 * @property delete Whether this proposal requests deletion of an existing manufacturer.
 * @property proposalComment Optional explanation provided by the author.
 * @property createdAt Timestamp when the proposal was submitted.
 * @property checkedBy ID of the reviewing user, or null if pending.
 * @property checkResult Review decision, or null if pending.
 * @property checkComment Optional explanation provided by the reviewer.
 * @property checkedAt Timestamp when the proposal was reviewed, or null if pending.
 */
data class ManufacturerProposal(
    val id: Long,
    val targetManufacturerId: Long?,
    val resultManufacturerId: Long?,
    val authorId: Uuid,
    val name: String,
    val longName: String?,
    val delete: Boolean,
    val proposalComment: String?,
    val createdAt: Instant,
    val checkedBy: Uuid?,
    val checkResult: CheckResult?,
    val checkComment: String?,
    val checkedAt: Instant?,
)