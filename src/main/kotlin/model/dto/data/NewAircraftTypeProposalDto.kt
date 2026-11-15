package org.ouraircraft.model.dto.data

import kotlinx.serialization.Serializable

/**
 * A proposal to add or modify an aircraft type.
 *
 * @property proposalComment A comment by the proposer.
 * @property manufacturerId The ID of the aircraft manufacturer.
 * @property family The aircraft type family.
 * @property variant The variant within the aircraft type family.
 * @property icaoDesignator The ICAO aircraft type designator.
 * @property engineCount The number of engines.
 * @property engineType The engine type.
 * @property wakeCategory The wake turbulence category.
 */
@Serializable
data class NewAircraftTypeProposalDto(
    val proposalComment: String?,
    val manufacturerId: Long,
    val family: String,
    val variant: String,
    val icaoDesignator: String,
    val engineCount: Int,
    val engineType: String,
    val wakeCategory: String,
)
