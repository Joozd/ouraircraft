package org.ouraircraft.db.repository.convertors

import org.jetbrains.exposed.v1.core.ResultRow
import org.ouraircraft.db.tables.AircraftTypeProposalTable
import org.ouraircraft.model.data.TypeProposal

/**
 * Maps a database result row to a [TypeProposal].
 *
 * Resolves foreign key references to their underlying IDs and
 * preserves nullable fields for pending or rejected proposals.
 */
fun ResultRow.toTypeProposal(): TypeProposal = TypeProposal(
    id = this[AircraftTypeProposalTable.id].value,
    targetAircraftTypeId = this[AircraftTypeProposalTable.targetAircraftTypeId]?.value,
    resultAircraftTypeId = this[AircraftTypeProposalTable.resultAircraftTypeId]?.value,
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
    checkComment = this[AircraftTypeProposalTable.checkResultComment],
    createdAt = this[AircraftTypeProposalTable.createdAt],
    checkedBy = this[AircraftTypeProposalTable.checkedBy],
    checkResult = this[AircraftTypeProposalTable.checkResult],
    checkedAt = this[AircraftTypeProposalTable.checkedAt],
)