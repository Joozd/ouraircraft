package org.ouraircraft.db.repository.convertors

import org.jetbrains.exposed.v1.core.ResultRow
import org.ouraircraft.db.tables.AircraftTypeProposalTable
import org.ouraircraft.model.data.AircraftTypeProposal

/**
 * Maps a database result row to an [AircraftTypeProposal].
 *
 * Resolves EntityID references to their underlying IDs and
 * preserves nullable fields for pending or rejected proposals.
 */
fun ResultRow.toAircraftTypeProposal(): AircraftTypeProposal = AircraftTypeProposal(
    id = this[AircraftTypeProposalTable.id].value,
    targetId = this[AircraftTypeProposalTable.targetAircraftTypeId]?.value,
    resultId = this[AircraftTypeProposalTable.resultAircraftTypeId]?.value,
    authorId = this[AircraftTypeProposalTable.authorId],
    manufacturerId = this[AircraftTypeProposalTable.manufacturerId].value,
    family = this[AircraftTypeProposalTable.family],
    variant = this[AircraftTypeProposalTable.variant],
    icaoDesignator = this[AircraftTypeProposalTable.icaoDesignator],
    engineCount = this[AircraftTypeProposalTable.engineCount],
    engineType = this[AircraftTypeProposalTable.engineType],
    wakeCategory = this[AircraftTypeProposalTable.wakeCategory],
    delete = this[AircraftTypeProposalTable.delete],
    proposalComment = this[AircraftTypeProposalTable.proposalComment],
    createdAt = this[AircraftTypeProposalTable.createdAt],
    checkedBy = this[AircraftTypeProposalTable.checkedBy],
    checkResult = this[AircraftTypeProposalTable.checkResult],
    checkResultComment = this[AircraftTypeProposalTable.checkResultComment],
    checkedAt = this[AircraftTypeProposalTable.checkedAt],
)