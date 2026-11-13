@file:Suppress("UnusedReceiverParameter", "UNUSED_PARAMETER")

package org.ouraircraft.db.repository.ops

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.ouraircraft.db.exceptions.ProposalNotFoundByIdException
import org.ouraircraft.db.exceptions.TargetItemForProposalNotFoundByIdException
import org.ouraircraft.db.helpers.startsWith
import org.ouraircraft.db.repository.ManufacturerRepository
import org.ouraircraft.db.repository.convertors.toManufacturer
import org.ouraircraft.db.repository.convertors.toManufacturerProposal
import org.ouraircraft.db.tables.ManufacturerProposalTable
import org.ouraircraft.db.tables.ManufacturerTable
import org.ouraircraft.model.auth.User
import org.ouraircraft.model.data.CheckResult
import org.ouraircraft.model.data.Manufacturer
import org.ouraircraft.model.data.ManufacturerProposal

/**
 * Finds manufacturers whose names start with [prefix].
 *
 * The prefix is normalized before matching.
 *
 * @return Matching manufacturers, or an empty list if none are found.
 */
context(tx: JdbcTransaction)
internal fun getManufacturersByNamePrefixInTransaction(prefix: String, maxMatches: Int): List<Manufacturer> =
    ManufacturerTable
        .selectAll()
        .where { ManufacturerTable.name startsWith prefix }
        .limit(maxMatches)
        .map { it.toManufacturer() }


/**
 * Gets the proposal with id [id], or null if not found.
 */
context(tx: JdbcTransaction)
internal fun ManufacturerRepository.getManufacturerProposalByIdInTransaction(id: Long): ManufacturerProposal? =
    ManufacturerProposalTable.selectAll()
        .where { ManufacturerProposalTable.id eq id }
        .singleOrNull()
        ?.toManufacturerProposal()


/**
 * Inserts a new Manufacturer proposal into the database.
 */
context(tx: JdbcTransaction)
internal fun ManufacturerRepository.insertManufacturerProposalInTransaction(proposal: ManufacturerProposal){
    ManufacturerProposalTable.insert { row ->
        row[ManufacturerProposalTable.targetManufacturerId] = proposal.targetId
        row[ManufacturerProposalTable.resultManufacturerId] = proposal.resultId
        row[ManufacturerProposalTable.authorId] = proposal.authorId

        row[name] = proposal.name
        row[longName] = proposal.longName

        row[ManufacturerProposalTable.delete] = proposal.delete
        row[ManufacturerProposalTable.proposalComment] = proposal.proposalComment
        row[ManufacturerProposalTable.createdAt] = proposal.createdAt

        row[ManufacturerProposalTable.checkedBy] = proposal.checkedBy
        row[ManufacturerProposalTable.checkResult] = proposal.checkResult
        row[ManufacturerProposalTable.checkResultComment] = proposal.checkResultComment
        row[ManufacturerProposalTable.checkedAt] = proposal.checkedAt
    }
}


/**
 * Gets all Manufacturer proposals awaiting review.
 * @return a list of all ManufacturerProposal in database where [ManufacturerProposalTable.checkResult] is null,
 *  ordered by time of creation (oldest first)
 */
context(tx: JdbcTransaction)
internal fun ManufacturerRepository.getAllOpenManufacturerProposalsInTransaction(): List<ManufacturerProposal> =
    ManufacturerProposalTable
        .selectAll()
        .where{ ManufacturerProposalTable.checkResult eq null }
        .orderBy(ManufacturerProposalTable.createdAt to SortOrder.ASC)
        .map { it.toManufacturerProposal() }



/**
 * Creates a Manufacturer from an AircraftProposal.
 * [proposal] does not need to include checking data at this point.
 */
context(tx: JdbcTransaction)
internal fun ManufacturerRepository.createManufacturerFromProposalInTransaction(proposal: ManufacturerProposal): Long =
    ManufacturerTable.insertAndGetId { row ->
        row[name] = proposal.name
        row[longName] = proposal.longName
        row[deleted] = false
    }.value


/**
 * Updates a Manufacturer from an AircraftProposal.
 * [proposal] does not need to include checking data at this point.
 * @throws ProposalNotFoundByIdException if no proposal is found with [proposal].id as its ID
 */
context(tx: JdbcTransaction)
internal fun ManufacturerRepository.updateManufacturerFromProposalInTransaction(proposal: ManufacturerProposal): Long {
    proposal.targetId!! // throws NullPointerException if target is null. This should have been checked before.
    val updatedRows = ManufacturerTable.update(where = { ManufacturerTable.id eq proposal.targetId }) { row ->
        row[name] = proposal.name
        row[longName] = proposal.longName
    }
    if(updatedRows == 0) throw TargetItemForProposalNotFoundByIdException(proposal.targetId, proposal.id)
    return proposal.targetId
}


/**
 * Creates a Manufacturer from an AircraftProposal.
 * [proposal] does not need to include checking data at this point.
 * This does NOT check if this proposal actually wants to delete.
 * @throws ProposalNotFoundByIdException if no proposal is found with [proposal].id as its ID
 */
context(tx: JdbcTransaction)
internal fun ManufacturerRepository.softDeleteManufacturerFromProposalInTransaction(proposal: ManufacturerProposal): Long {
    proposal.targetId!! // throws NullPointerException if target is null. This should have been checked before.
    val updatedRows = ManufacturerTable.update(where = { ManufacturerTable.id eq proposal.targetId }) { row ->
        row[deleted] = true
    }
    if(updatedRows == 0) throw TargetItemForProposalNotFoundByIdException(proposal.targetId, proposal.id)
    return proposal.targetId
}



/**
 * Marks the proposal as checked.
 * @param id The PK of the Proposal that is being updated
 * @param checkResult The result of the check
 * @param user The User that did this check
 * @param manufacturerId The resulting Manufacturer ID, or null if checkResult is REJECTED. This is required and checked.
 *
 * @throws ProposalNotFoundByIdException if no proposal with this ID is found
 * @throws IllegalArgumentException if an approved proposal does not have a manufacturerId, or a rejected does.
 */
context(tx: JdbcTransaction)
internal fun ManufacturerRepository.markManufacturerProposalAsCheckedInTransaction(id: Long, checkResult: CheckResult, user: User, comment: String?, manufacturerId: Long? = null) {
    // cement audit log completeness
    when (checkResult) {
        CheckResult.APPROVED -> require(manufacturerId != null) { "An approved proposal MUST have a result ID" }
        CheckResult.REJECTED -> require(manufacturerId == null) { "A rejected proposal MUST NOT have a result ID" }
    }

    ManufacturerProposalTable.updateFromCheck(
        where = { ManufacturerProposalTable.id eq id },
        user,
        checkResult,
        comment
    ) { row ->
        row[resultManufacturerId] = manufacturerId
    }
}