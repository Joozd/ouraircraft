package org.ouraircraft.model.dto.data

import kotlinx.serialization.Serializable

/**
 * A proposal to add or modify an aircraft.
 *
 * @property proposalComment A comment by the proposer.
 * @property aircraftTypeId The ID of the aircraft type.
 * @property registration The aircraft registration.
 */
@Serializable
data class NewAircraftProposalDto(
    val proposalComment: String?,
    val aircraftTypeId: Long,
    val registration: String,
)
