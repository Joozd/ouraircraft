package org.ouraircraft.model.data

import org.ouraircraft.model.auth.User
import org.ouraircraft.model.data.valueclasses.AircraftRegistration
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Represents a proposal to create, modify, or delete an aircraft.
 *
 * Contains a complete snapshot of the proposed aircraft data.
 * Once created, proposal data is immutable. Review fields may be populated
 * once when the proposal is accepted or rejected.
 *
 * @property id Unique identifier of this proposal.
 * @property targetId ID of the aircraft being modified or deleted,
 *   or null if proposing a new aircraft.
 * @property resultId ID of the resulting aircraft after acceptance,
 *   or null if pending or rejected.
 * @property authorId ID of the user who submitted the proposal.
 * @property aircraftTypeId ID of the aircraft type associated with the proposed aircraft.
 * @property registration Normalized aircraft registration (uppercase ASCII letters and digits).
 * @property delete Whether this proposal requests deletion of an existing aircraft.
 * @property proposalComment Optional explanation provided by the author.
 * @property createdAt Timestamp when the proposal was submitted.
 * @property checkedBy ID of the reviewing user, or null if pending.
 * @property checkResult Review decision, or null if pending.
 * @property checkResultComment Optional explanation provided by the reviewer.
 * @property checkedAt Timestamp when the proposal was reviewed, or null if pending.
 */
data class AircraftProposal(
    override val id: Long,
    override val targetId: Long?,
    override val resultId: Long?,
    override val authorId: Uuid,
    override val delete: Boolean,
    override val proposalComment: String?,
    override val createdAt: Instant,
    override val checkedBy: Uuid?,
    override val checkResult: CheckResult?,
    override val checkResultComment: String?,
    override val checkedAt: Instant?,
    val aircraftTypeId: Long,
    val registration: AircraftRegistration,
): Proposal {
    constructor(
        user: User,
        targetId: Long?,
        delete: Boolean,
        proposalComment: String?,
        aircraftTypeId: Long,
        registration: AircraftRegistration
    ): this(
        id = -1,
        targetId = targetId,
        resultId = null,
        authorId = user.id,
        delete = delete,
        proposalComment = proposalComment,
        createdAt = Clock.System.now(),
        checkedBy = null,
        checkResult = null,
        checkResultComment = null,
        checkedAt = null,
        aircraftTypeId = aircraftTypeId,
        registration = registration
    )
    init{
        checkSanity()
    }
}