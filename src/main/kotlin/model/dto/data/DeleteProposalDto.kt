package org.ouraircraft.model.dto.data

import kotlinx.serialization.Serializable

/**
 * A proposal to delete a Manufacturer.
 * @param proposalComment A comment by the proposer
 *
 */
@Serializable
data class DeleteManufacturerProposalDto(
    val proposalComment: String?,
)

/**
 * A proposal to delete an aircraft type.
 *
 * @property proposalComment A comment by the proposer.
 */
@Serializable
data class DeleteAircraftTypeProposalDto(
    val proposalComment: String?,
)

/**
 * A proposal to delete an aircraft.
 *
 * @property proposalComment A comment by the proposer.
 */
@Serializable
data class DeleteAircraftProposalDto(
    val proposalComment: String?,
)