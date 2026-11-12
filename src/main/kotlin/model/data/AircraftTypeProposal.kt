package org.ouraircraft.model.data

import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Represents a proposal to create, modify, or delete an aircraft type.
 *
 * Contains a complete snapshot of the proposed aircraft type data.
 * Once created, proposal data is immutable. Review fields may be populated
 * once when the proposal is accepted or rejected.
 *
 * @property id Unique identifier of this proposal.
 * @property targetId ID of the aircraft type being modified or deleted,
 *   or null if proposing a new aircraft type.
 * @property resultId ID of the resulting aircraft type after acceptance,
 *   or null if pending or rejected.
 * @property authorId ID of the user who submitted the proposal.
 * @property manufacturerId ID of the manufacturer associated with the proposed type.
 * @property family Aircraft family name.
 * @property variant Aircraft type variant.
 * @property icaoDesignator ICAO aircraft type designator.
 * @property engineCount Number of engines.
 * @property engineType Type of engines.
 * @property wakeCategory Wake turbulence category.
 * @property delete Whether this proposal requests deletion of an existing aircraft type.
 * @property proposalComment Optional explanation provided by the author.
 * @property checkResultComment Optional explanation provided by the reviewer.
 * @property createdAt Timestamp when the proposal was submitted.
 * @property checkedBy ID of the reviewing user, or null if pending.
 * @property checkResult Review decision, or null if pending.
 * @property checkedAt Timestamp when the proposal was reviewed, or null if pending.
 */
data class AircraftTypeProposal(
    override val id: Long,
    override val targetId: Long?,
    override val resultId: Long?,
    override val authorId: Uuid,
    val manufacturerId: Long,
    val family: String,
    val variant: String,
    val icaoDesignator: String,
    val engineCount: Int,
    val engineType: String,
    val wakeCategory: String,
    override val delete: Boolean,
    override val proposalComment: String?,
    override val checkResultComment: String?,
    override val createdAt: Instant,
    override val checkedBy: Uuid?,
    override val checkResult: CheckResult?,
    override val checkedAt: Instant?,
): Proposal {
    init{
        checkSanity()
    }
}