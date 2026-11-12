@file:Suppress("UnusedReceiverParameter", "UNUSED_PARAMETER")

package org.ouraircraft.db.repository.ops

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.*
import org.ouraircraft.db.exceptions.ProposalNotFoundByIdException
import org.ouraircraft.db.exceptions.TargetItemForProposalNotFoundByIdException
import org.ouraircraft.db.helpers.startsWith
import org.ouraircraft.db.repository.AircraftTypeRepository
import org.ouraircraft.db.repository.convertors.toAircraftType
import org.ouraircraft.db.repository.convertors.toAircraftTypeProposal
import org.ouraircraft.db.tables.AircraftTypeProposalTable
import org.ouraircraft.db.tables.AircraftTypeTable
import org.ouraircraft.model.auth.User
import org.ouraircraft.model.data.AircraftType
import org.ouraircraft.model.data.AircraftTypeProposal
import org.ouraircraft.model.data.CheckResult

// TODO move to settings
private const val MAX_PREFIX_MATCHES = 20

/**
 * Finds aircraft types whose family names start with [prefix].
 *
 * Depending on the database implementation, this should not be case-sensitive.
 * @see startsWith for normalizing behavior
 *
 * @return Matching aircraft types, or an empty list if none are found.
 */
context(tx: JdbcTransaction)
internal fun getAircraftTypesByFamilyPrefixInTransaction(
    prefix: String,
    maxMatches: Int = MAX_PREFIX_MATCHES
): List<AircraftType> =
    AircraftTypeTable
        .selectAll()
        .where { AircraftTypeTable.family startsWith prefix }
        .limit(maxMatches)
        .map { it.toAircraftType() }


/**
 * Finds aircraft types whose ICAO designator starts with [prefix].
 *
 * Depending on the database implementation, this should not be case-sensitive.
 * @see startsWith for normalizing behavior
 *
 * @return Matching aircraft types, or an empty list if none are found.
 */
context(tx: JdbcTransaction)
internal fun getAircraftTypesByDesignatorPrefixInTransaction(
    prefix: String,
    maxMatches: Int = MAX_PREFIX_MATCHES
): List<AircraftType> =
    AircraftTypeTable
        .selectAll()
        .where { AircraftTypeTable.icaoDesignator startsWith prefix }
        .limit(maxMatches)
        .map { it.toAircraftType() }


/**
 * Gets the proposal with id [id], or null if not found.
 */
context(tx: JdbcTransaction)
internal fun AircraftTypeRepository.getAircraftTypeProposalByIdInTransaction(id: Long): AircraftTypeProposal? =
    AircraftTypeProposalTable.selectAll()
        .where { AircraftTypeProposalTable.id eq id }
        .singleOrNull()
        ?.toAircraftTypeProposal()


/**
 * Inserts a new AircraftType proposal into the database.
 */
context(tx: JdbcTransaction)
internal fun AircraftTypeRepository.insertAircraftTypeProposalInTransaction(proposal: AircraftTypeProposal) {
    AircraftTypeProposalTable.insert { row ->
        row[AircraftTypeProposalTable.targetAircraftTypeId] = proposal.targetId
        row[AircraftTypeProposalTable.resultAircraftTypeId] = proposal.resultId
        row[AircraftTypeProposalTable.authorId] = proposal.authorId

        row[manufacturerId] = proposal.manufacturerId
        row[family] = proposal.family
        row[variant] = proposal.variant
        row[icaoDesignator] = proposal.icaoDesignator
        row[engineCount] = proposal.engineCount
        row[engineType] = proposal.engineType
        row[wakeCategory] = proposal.wakeCategory

        row[AircraftTypeProposalTable.delete] = proposal.delete
        row[AircraftTypeProposalTable.proposalComment] = proposal.proposalComment
        row[AircraftTypeProposalTable.createdAt] = proposal.createdAt

        row[AircraftTypeProposalTable.checkedBy] = proposal.checkedBy
        row[AircraftTypeProposalTable.checkResult] = proposal.checkResult
        row[AircraftTypeProposalTable.checkResultComment] = proposal.checkResultComment
        row[AircraftTypeProposalTable.checkedAt] = proposal.checkedAt
    }
}


/**
 * Gets all AircraftType proposals awaiting review.
 *
 * @return All proposals where [AircraftTypeProposalTable.checkResult] is null,
 * ordered by time of creation (oldest first).
 */
context(tx: JdbcTransaction)
internal fun AircraftTypeRepository.getAllOpenAircraftTypeProposalsInTransaction(): List<AircraftTypeProposal> =
    AircraftTypeProposalTable
        .selectAll()
        .where { AircraftTypeProposalTable.checkResult eq null }
        .orderBy(AircraftTypeProposalTable.createdAt to SortOrder.ASC)
        .map { it.toAircraftTypeProposal() }


/**
 * Creates an AircraftType from an AircraftTypeProposal.
 *
 * [proposal] does not need to include review data at this point.
 */
context(tx: JdbcTransaction)
internal fun AircraftTypeRepository.createAircraftTypeFromProposalInTransaction(proposal: AircraftTypeProposal): Long =
    AircraftTypeTable.insertAndGetId { row ->
        row[manufacturerId] = proposal.manufacturerId
        row[family] = proposal.family
        row[variant] = proposal.variant
        row[icaoDesignator] = proposal.icaoDesignator
        row[engineCount] = proposal.engineCount
        row[engineType] = proposal.engineType
        row[wakeCategory] = proposal.wakeCategory
        row[deleted] = false
    }.value


/**
 * Updates an AircraftType from an AircraftTypeProposal.
 *
 * [proposal] does not need to include review data at this point.
 *
 * @throws NullPointerException if [proposal] has no target ID.
 * @throws TargetItemForProposalNotFoundByIdException if the target aircraft type does not exist.
 */
context(tx: JdbcTransaction)
internal fun AircraftTypeRepository.updateAircraftTypeFromProposalInTransaction(proposal: AircraftTypeProposal): Long {
    val targetId = proposal.targetId!!
    val updatedRows = AircraftTypeTable.update(where = { AircraftTypeTable.id eq targetId }) { row ->
        row[manufacturerId] = proposal.manufacturerId
        row[family] = proposal.family
        row[variant] = proposal.variant
        row[icaoDesignator] = proposal.icaoDesignator
        row[engineCount] = proposal.engineCount
        row[engineType] = proposal.engineType
        row[wakeCategory] = proposal.wakeCategory
    }
    if (updatedRows == 0) throw TargetItemForProposalNotFoundByIdException(targetId, proposal.id)
    return targetId
}


/**
 * Marks the target aircraft type as deleted.
 *
 * [proposal] does not need to include review data at this point.
 * Does not check whether the proposal requests deletion.
 *
 * @throws NullPointerException if [proposal] has no target ID.
 * @throws TargetItemForProposalNotFoundByIdException if the target aircraft type does not exist.
 */
context(tx: JdbcTransaction)
internal fun AircraftTypeRepository.softDeleteAircraftTypeFromProposalInTransaction(proposal: AircraftTypeProposal): Long {
    val targetId = proposal.targetId!!
    val updatedRows = AircraftTypeTable.update(where = { AircraftTypeTable.id eq targetId }) { row ->
        row[deleted] = true
    }
    if (updatedRows == 0) throw TargetItemForProposalNotFoundByIdException(targetId, proposal.id)
    return targetId
}


/**
 * Marks the proposal as checked.
 *
 * @param id The PK of the proposal that is being updated.
 * @param checkResult The result of the check.
 * @param user The user that performed this check.
 * @param comment Optional explanation provided by the reviewer.
 * @param aircraftTypeId The resulting AircraftType ID, or null if rejected.
 *
 * @throws ProposalNotFoundByIdException if no proposal with this ID is found.
 * @throws IllegalArgumentException if an approved proposal does not have an
 * aircraftTypeId, or a rejected proposal does.
 */
context(tx: JdbcTransaction)
internal fun AircraftTypeRepository.markAircraftTypeProposalAsCheckedInTransaction(
    id: Long,
    checkResult: CheckResult,
    user: User,
    comment: String?,
    aircraftTypeId: Long? = null
) {
    // cement audit log completeness
    when (checkResult) {
        CheckResult.APPROVED -> require(aircraftTypeId != null) {
            "An approved proposal MUST have a result ID"
        }
        CheckResult.REJECTED -> require(aircraftTypeId == null) {
            "A rejected proposal MUST NOT have a result ID"
        }
    }

    AircraftTypeProposalTable.updateFromCheck(
        where = { AircraftTypeProposalTable.id eq id },
        user,
        checkResult,
        comment
    ) { row ->
        row[resultAircraftTypeId] = aircraftTypeId
    }
}