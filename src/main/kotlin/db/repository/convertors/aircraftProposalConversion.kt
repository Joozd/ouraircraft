package org.ouraircraft.db.repository.convertors

import org.jetbrains.exposed.v1.core.ResultRow
import org.ouraircraft.db.tables.AircraftProposalTable
import org.ouraircraft.model.data.AircraftProposal

/**
 * Maps a database result row to an [AircraftProposal].
 *
 * Resolves EntityID references to their underlying IDs and
 * preserves nullable fields for pending or rejected proposals.
 */
fun ResultRow.toAircraftProposal(): AircraftProposal = AircraftProposal(
    id = this[AircraftProposalTable.id].value,
    targetAircraftId = this[AircraftProposalTable.targetAircraftId]?.value,
    resultAircraftId = this[AircraftProposalTable.resultAircraftId]?.value,
    authorId = this[AircraftProposalTable.authorId],
    aircraftTypeId = this[AircraftProposalTable.aircraftTypeId].value,
    registration = this[AircraftProposalTable.registration],
    delete = this[AircraftProposalTable.delete],
    proposalComment = this[AircraftProposalTable.proposalComment],
    createdAt = this[AircraftProposalTable.createdAt],
    checkedBy = this[AircraftProposalTable.checkedBy],
    checkResult = this[AircraftProposalTable.checkResult],
    checkResultComment = this[AircraftProposalTable.checkResultComment],
    checkedAt = this[AircraftProposalTable.checkedAt],
)