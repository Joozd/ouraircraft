package org.ouraircraft.model.dto.data

import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.Uuid


/**
 * Represents an unchecked proposal to create, modify, or delete an aircraft type.
 *
 * Contains a complete snapshot of the proposed aircraft data.
 * Once created, proposal data is immutable. Review fields may be populated
 * once when the proposal is accepted or rejected.
 *
 * @property id Unique identifier of this proposal.
 * @property targetId ID of the aircraft being modified or deleted,
 *   or null if proposing a new aircraft.
 *   or null if pending or rejected.
 * @property authorId ID of the user who submitted the proposal.
 * @property delete Whether this proposal requests deletion of an existing aircraft.
 * @property proposalComment Optional explanation provided by the author.
 * @property createdAt Timestamp when the proposal was submitted.
 * @property manufacturerId ID of the manufacturer associated with the proposed type.
 * @property family Aircraft family name.
 * @property variant Aircraft type variant.
 * @property icaoDesignator ICAO aircraft type designator.
 * @property engineCount Number of engines.
 * @property engineType Type of engines.
 * @property wakeCategory Wake turbulence category.
 */
@Serializable
data class UncheckedAircraftTypeProposalDto(
    val id: Long,
    val targetId: Long?,
    val authorId: Uuid,
    val delete: Boolean,
    val proposalComment: String?,
    val createdAt: Instant,
    val manufacturerId: Long,
    val family: String,
    val variant: String,
    val icaoDesignator: String,
    val engineCount: Int,
    val engineType: String,
    val wakeCategory: String,
)
