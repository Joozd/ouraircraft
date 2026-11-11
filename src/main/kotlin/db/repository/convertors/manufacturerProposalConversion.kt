package org.ouraircraft.db.repository.convertors

import org.jetbrains.exposed.v1.core.ResultRow
import org.ouraircraft.db.tables.ManufacturerProposalTable
import org.ouraircraft.model.data.ManufacturerProposal

/**
 * Maps a database result row to a [ManufacturerProposal].
 *
 * Resolves EntityID references to their underlying IDs and
 * preserves nullable fields for pending or rejected proposals.
 */
fun ResultRow.toManufacturerProposal(): ManufacturerProposal = ManufacturerProposal(
    id = this[ManufacturerProposalTable.id].value,
    targetManufacturerId = this[ManufacturerProposalTable.targetManufacturerId]?.value,
    resultManufacturerId = this[ManufacturerProposalTable.resultManufacturerId]?.value,
    authorId = this[ManufacturerProposalTable.authorId],
    name = this[ManufacturerProposalTable.name],
    longName = this[ManufacturerProposalTable.longName],
    delete = this[ManufacturerProposalTable.delete],
    proposalComment = this[ManufacturerProposalTable.proposalComment],
    createdAt = this[ManufacturerProposalTable.createdAt],
    checkedBy = this[ManufacturerProposalTable.checkedBy],
    checkResult = this[ManufacturerProposalTable.checkResult],
    checkComment = this[ManufacturerProposalTable.checkResultComment],
    checkedAt = this[ManufacturerProposalTable.checkedAt],
)