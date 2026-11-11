package org.ouraircraft.model.data

import org.ouraircraft.model.data.valueclasses.AircraftRegistration
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
 * @property targetAircraftId ID of the aircraft being modified or deleted,
 *   or null if proposing a new aircraft.
 * @property resultAircraftId ID of the resulting aircraft after acceptance,
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
    val id: Long,
    val targetAircraftId: Long?,
    val resultAircraftId: Long?,
    val authorId: Uuid,
    val aircraftTypeId: Long,
    val registration: AircraftRegistration,
    val delete: Boolean,
    val proposalComment: String?,
    val createdAt: Instant,
    val checkedBy: Uuid?,
    val checkResult: CheckResult?,
    val checkResultComment: String?,
    val checkedAt: Instant?,
)