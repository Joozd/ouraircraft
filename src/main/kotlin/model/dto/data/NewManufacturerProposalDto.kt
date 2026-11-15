package org.ouraircraft.model.dto.data

import kotlinx.serialization.Serializable

/**
 * A proposal to add, delete or modify a Manufacturer.
 * @param proposalComment A comment by the proposer
 * @param name The short name of the Manufacturer (e.g. "Boeing")
 * @param longName a longer version of that name (e.g. "The Boeing Company")
 *
 */
@Serializable
data class NewManufacturerProposalDto(
    val proposalComment: String?,
    val name: String,
    val longName: String?
)
