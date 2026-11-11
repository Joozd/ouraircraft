@file:Suppress("UNUSED_PARAMETER")

package org.ouraircraft.db.repository.ops

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.ouraircraft.db.exceptions.ProposalNotFoundByIdException
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

private const val MAX_PREFIX_MATCHES = 20

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
 * Finds aircraft whose registrations start with [prefix].
 *
 * Includes the corresponding aircraft type and manufacturer in a single query.
 * The prefix is normalized before matching.
 *
 * @return Matching aircraft, or an empty list if none are found.
 */
context(tx: JdbcTransaction)
internal fun getAircraftByRegistrationPrefixInTransaction(prefix: String): List<Aircraft> =
    AircraftTable
        .innerJoin(AircraftTypeTable)
        .innerJoin(ManufacturerTable)
        .selectAll()
        .where { AircraftTable.registration startsWith prefix }
        .limit(MAX_PREFIX_MATCHES)
        .map { it.toAircraft() }


/**
 * Inserts a new aircraft proposal into the database.
 */
context(tx: JdbcTransaction)
internal fun insertAircraftProposalInTransaction(proposal: AircraftProposal) {
    AircraftProposalTable.insert { row ->
        row[AircraftProposalTable.targetAircraftId] = proposal.targetAircraftId
        row[AircraftProposalTable.resultAircraftId] = proposal.resultAircraftId
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
 * Gets all aircraft proposals awaiting review.
 * @return a list of all AircraftProposal in database where [AircraftProposalTable.createdAt] is null,
 *  ordered by time of creation (oldest first)
 */
context(tx: JdbcTransaction)
internal fun AircraftRepository.getAllOpenAircraftProposalsInTransaction(): List<AircraftProposal> =
    AircraftProposalTable
        .selectAll()
        .where{ AircraftProposalTable.checkResult eq null }
        .orderBy(AircraftProposalTable.createdAt to SortOrder.ASC)
        .map { it.toAircraftProposal() }


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

/**
 * Creates an Aircraft from an AircraftProposal.
 * [proposal] does not need to include checking data at this point.
 */
context(tx: JdbcTransaction)
internal fun AircraftRepository.createAircraftFromProposalInTransaction(proposal: AircraftProposal): Long =
    AircraftTable.insertAndGetId { row ->
        row[aircraftTypeId] = proposal.aircraftTypeId
        row[registration] = proposal.registration
    }.value
