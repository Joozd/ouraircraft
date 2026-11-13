@file:Suppress("UNUSED_PARAMETER", "UnusedReceiverParameter")

package org.ouraircraft.db.repository.ops

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.ouraircraft.db.exceptions.ProposalNotFoundByIdException
import org.ouraircraft.db.exceptions.TargetItemForProposalNotFoundByIdException
import org.ouraircraft.db.helpers.startsWith
import org.ouraircraft.db.repository.AircraftRepository
import org.ouraircraft.db.repository.convertors.toAircraft
import org.ouraircraft.db.repository.convertors.toAircraftProposal
import org.ouraircraft.db.tables.AircraftProposalTable
import org.ouraircraft.db.tables.AircraftTable
import org.ouraircraft.db.tables.AircraftTypeTable
import org.ouraircraft.db.tables.ManufacturerTable
import org.ouraircraft.model.auth.User
import org.ouraircraft.model.data.Aircraft
import org.ouraircraft.model.data.AircraftProposal
import org.ouraircraft.model.data.CheckResult
import org.ouraircraft.model.data.PagedResult

/**
 * Finds an aircraft by its database ID, including its type and manufacturer.
 *
 * @return The aircraft, or null if no matching record exists.
 */
context(tx: JdbcTransaction)
internal fun findAircraftByIdInTransaction(id: Long): Aircraft? =
    AircraftTable
        .innerJoin(AircraftTypeTable)
        .innerJoin(ManufacturerTable)
        .selectAll()
        .where { AircraftTable.id eq id }
        .singleOrNull()
        ?.toAircraft()


/**
 * Finds a page of aircraft whose registrations start with [prefix].
 *
 * Includes the corresponding aircraft type and manufacturer in the results query.
 * The prefix is normalized before matching.
 *
 * @param prefix The registration prefix to match.
 * @param maxMatches The maximum number of results to return.
 * @param offset The zero-based number of matching results to skip.
 * @return A page of matching aircraft, ordered by registration ascending
 * and ID descending, including the total number of matches before pagination.
 */
context(tx: JdbcTransaction)
internal fun getAircraftByRegistrationPrefixInTransaction(
    prefix: String,
    maxMatches: Int,
    offset: Long
): PagedResult<Aircraft> {
    val query = AircraftTable
        .innerJoin(AircraftTypeTable)
        .innerJoin(ManufacturerTable)
        .selectAll()
        .where { AircraftTable.registration startsWith prefix }
        .orderBy(AircraftTable.registration to SortOrder.ASC, AircraftTable.id to SortOrder.DESC)

    val totalCount = query.count()

    val results = query
        .limit(maxMatches)
        .offset(offset)
        .map { it.toAircraft() }

    return PagedResult(results, maxMatches, offset, totalCount)
}


/**
 * Inserts a new aircraft proposal into the database.
 */
context(tx: JdbcTransaction)
internal fun insertAircraftProposalInTransaction(proposal: AircraftProposal) {
    AircraftProposalTable.insert { row ->
        row[AircraftProposalTable.targetAircraftId] = proposal.targetId
        row[AircraftProposalTable.resultAircraftId] = proposal.resultId
        row[AircraftProposalTable.authorId] = proposal.authorId
        row[AircraftProposalTable.aircraftTypeId] = proposal.aircraftTypeId

        row[AircraftProposalTable.registration] = proposal.registration
        row[AircraftProposalTable.delete] = proposal.delete
        row[AircraftProposalTable.proposalComment] = proposal.proposalComment
        row[AircraftProposalTable.createdAt] = proposal.createdAt
        row[AircraftProposalTable.checkedBy] = proposal.checkedBy
        row[AircraftProposalTable.checkResult] = proposal.checkResult
        row[AircraftProposalTable.checkResultComment] = proposal.checkResultComment
        row[AircraftProposalTable.checkedAt] = proposal.checkedAt
    }
}

/**
 * Gets a page of aircraft proposals awaiting review.
 *
 * @param maxMatches The maximum number of results to return.
 * @param offset The zero-based number of matching results to skip.
 * @return A page of proposals where [AircraftProposalTable.checkResult] is null,
 * ordered by creation time ascending and ID descending, including the total
 * number of matches before pagination.
 */
context(tx: JdbcTransaction)
internal fun AircraftRepository.getOpenAircraftProposalsInTransaction(
    maxMatches: Int,
    offset: Long
): PagedResult<AircraftProposal> {
    val query = AircraftProposalTable
        .selectAll()
        .where { AircraftProposalTable.checkResult eq null }
        .orderBy(AircraftProposalTable.createdAt to SortOrder.ASC, AircraftProposalTable.id to SortOrder.DESC)

    val totalCount = query.count()

    val results = query
        .limit(maxMatches)
        .offset(offset)
        .map { it.toAircraftProposal() }

    return PagedResult(results, maxMatches, offset, totalCount)
}


/**
 * Gets the proposal with id [id], or null if not found.
 */
context(tx: JdbcTransaction)
internal fun AircraftRepository.getAircraftProposalByIdInTransaction(id: Long): AircraftProposal? =
    AircraftProposalTable.selectAll()
        .where { AircraftProposalTable.id eq id }
        .singleOrNull()
        ?.toAircraftProposal()

/**
 * Creates an Aircraft from an AircraftProposal.
 *
 * [proposal] does not need to include review data at this point.
 */
context(tx: JdbcTransaction)
internal fun AircraftRepository.createAircraftFromProposalInTransaction(proposal: AircraftProposal): Long =
    AircraftTable.insertAndGetId { row ->
        row[aircraftTypeId] = proposal.aircraftTypeId
        row[registration] = proposal.registration
        row[deleted] = false
    }.value


/**
 * Updates an Aircraft from an AircraftProposal.
 *
 * [proposal] does not need to include review data at this point.
 *
 * @throws NullPointerException if [proposal] has no target ID.
 * @throws TargetItemForProposalNotFoundByIdException if the target aircraft does not exist.
 */
context(tx: JdbcTransaction)
internal fun AircraftRepository.updateAircraftFromProposalInTransaction(proposal: AircraftProposal): Long {
    val targetId = proposal.targetId!!
    val updatedRows = AircraftTable.update(where = { AircraftTable.id eq targetId }) { row ->
        row[aircraftTypeId] = proposal.aircraftTypeId
        row[registration] = proposal.registration
    }
    if (updatedRows == 0) throw TargetItemForProposalNotFoundByIdException(targetId, proposal.id)
    return targetId
}

/**
 * Marks the target aircraft as deleted.
 *
 * [proposal] does not need to include review data at this point.
 * Does not check whether the proposal requests deletion.
 *
 * @throws NullPointerException if [proposal] has no target ID.
 * @throws TargetItemForProposalNotFoundByIdException if the target aircraft does not exist.
 */
context(tx: JdbcTransaction)
internal fun AircraftRepository.softDeleteAircraftFromProposalInTransaction(proposal: AircraftProposal): Long {
    val targetId = proposal.targetId!!
    val updatedRows = AircraftTable.update(where = { AircraftTable.id eq targetId }) { row ->
        row[deleted] = true
    }
    if (updatedRows == 0) throw TargetItemForProposalNotFoundByIdException(targetId, proposal.id)
    return targetId
}

/**
 * Marks the proposal as checked.
 * @param id The PK of the Proposal that is being updated
 * @param checkResult The result of the check
 * @param user The User that did this check
 * @param aircraftId The resulting Aircraft ID, or null if checkResult is REJECT. This is required and checked.
 *
 * @throws ProposalNotFoundByIdException if no proposal with this ID is found
 * @throws IllegalArgumentException if an approved proposal does not have an aircraftId, or a rejected does.
 */
context(tx: JdbcTransaction)
internal fun AircraftRepository.markAircraftProposalAsCheckedInTransaction(id: Long, checkResult: CheckResult, user: User, comment: String?, aircraftId: Long? = null){
    // cement audit log completeness
    when(checkResult){
        CheckResult.APPROVED -> require(aircraftId != null) { "An approved proposal MUST have a result ID" }
        CheckResult.REJECTED -> require(aircraftId == null) { "A rejected proposal MUST NOT have a result ID" }
    }

    AircraftProposalTable.updateFromCheck (
        where = { AircraftProposalTable.id eq id},
        user,
        checkResult,
        comment
    ) { row ->
        row[resultAircraftId] = aircraftId
    }
}
