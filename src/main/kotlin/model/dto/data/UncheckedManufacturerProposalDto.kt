package org.ouraircraft.model.dto.data

import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Represents an unchecked proposal to create, modify, or delete a Manufacturer.
 *
 * Contains a complete snapshot of the proposed aircraft data.
 * Once created, proposal data is immutable. Review fields may be populated
 * once when the proposal is accepted or rejected.
 *
 * @property id Unique identifier of this proposal.
 * @property targetId ID of the aircraft being modified or deleted,
 *   or null if proposing a new aircraft.
 * @property authorId ID of the user who submitted the proposal.
 * @property name Proposed manufacturer name.
 * @property longName Optional full manufacturer name.
 * @property delete Whether this proposal requests deletion of an existing aircraft.
 * @property proposalComment Optional explanation provided by the author.
 * @property createdAt Timestamp when the proposal was submitted.
 */
@Serializable
data class UncheckedManufacturerProposalDto(
    val id: Long,
    val targetId: Long?,
    val authorId: Uuid,
    val delete: Boolean,
    val proposalComment: String?,
    val createdAt: Instant,
    val name: String,
    val longName: String?,
)

