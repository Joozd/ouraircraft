package org.ouraircraft.db.repository

import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.ouraircraft.auth.UserAuthorization
import org.ouraircraft.db.Database
import org.ouraircraft.db.exceptions.AlreadyCheckedException
import org.ouraircraft.db.exceptions.ProposalNotFoundByIdException
import org.ouraircraft.db.repository.ops.createAircraftTypeFromProposalInTransaction
import org.ouraircraft.db.repository.ops.getAllOpenAircraftTypeProposalsInTransaction
import org.ouraircraft.db.repository.ops.getAircraftTypeProposalByIdInTransaction
import org.ouraircraft.db.repository.ops.getAircraftTypesByFamilyPrefixInTransaction
import org.ouraircraft.db.repository.ops.insertAircraftTypeProposalInTransaction
import org.ouraircraft.db.repository.ops.markAircraftTypeProposalAsCheckedInTransaction
import org.ouraircraft.model.auth.User
import org.ouraircraft.model.data.CheckResult
import org.ouraircraft.model.data.AircraftType
import org.ouraircraft.model.data.AircraftTypeProposal
import org.ouraircraft.model.data.Proposal

class AircraftTypeRepository(val database: Database) {
    /**
     * Finds aircraft types whose names start with [prefix].
     *
     * The prefix is normalized before matching.
     *
     * @return Matching aircraft types, or an empty list if none are found.
     */
    fun getAircraftTypesByFamilyPrefix(prefix: String, user: User): List<AircraftType> =
        transaction(database.exposed) {
            user.requireAuthorization(UserAuthorization.DISPLAY_AIRCRAFT_TYPE)
            getAircraftTypesByFamilyPrefixInTransaction(prefix)
        }

    /**
     * Finds aircraft types whose names start with [prefix].
     *
     * The prefix is normalized before matching.
     *
     * @return Matching aircraft types, or an empty list if none are found.
     */
    fun getAircraftTypesByDesignatorPrefix(prefix: String, user: User): List<AircraftType> =
        transaction(database.exposed) {
            user.requireAuthorization(UserAuthorization.DISPLAY_AIRCRAFT_TYPE)
            getAircraftTypesByDesignatorPrefixInTransaction(prefix)
        }


    /**
     * Insert a new AircraftType proposal into the database.
     * The user mentioned in the proposal needs to be checked/inserted by the service calling this;
     * this wil store 'as is'.
     */
    fun insertAircraftTypeProposal(proposal: AircraftTypeProposal, user: User) =
        transaction(database.exposed) {
            user.requireAuthorization(UserAuthorization.PROPOSE_AIRCRAFT_TYPE)

            insertAircraftTypeProposalInTransaction(proposal)
        }


    /**
     * Get all currently outstanding AircraftType Proposals
     */
    fun getAllOpenAircraftTypeProposals(user: User): List<AircraftTypeProposal> =
        transaction(database.exposed) {
            user.requireAuthorization(UserAuthorization.DISPLAY_PROPOSED_AIRCRAFT_TYPES)
            getAllOpenAircraftTypeProposalsInTransaction()
        }

    /**
     * Handles the result of reviewing an AircraftType proposal.
     *
     * In a single transaction, updates the proposal's review fields and,
     * if approved, creates, updates, or soft-deletes the aircraft type.
     *
     * @param proposalId ID of the proposal being reviewed.
     * @param checkResult Review decision.
     * @param comment Optional explanation provided by the reviewer.
     * @param user User who performed the review.
     *
     * @throws org.ouraircraft.auth.exceptions.UserNotAuthorizedForThisException if [user] is not authorized for this action.
     * @throws AlreadyCheckedException if the proposal was already reviewed.
     * @throws ProposalNotFoundByIdException if the proposal does not exist.
     * @throws TargetItemForProposalNotFoundByIdException if the aircraft type to update or soft-delete does not exist.
     */
    fun checkAircraftTypeProposal(proposalId: Long, checkResult: CheckResult, comment: String?, user: User) {
        user.requireAuthorization(UserAuthorization.DECIDE_ON_PROPOSED_AIRCRAFT_TYPE)

        transaction(database.exposed) {
            // check if this proposal (still) needs to be checked
            val proposal = getAircraftTypeProposalByIdInTransaction(proposalId)
                ?.also { if (it.checkResult != null) throw AlreadyCheckedException("AircraftTypeProposal $proposalId has already been checked.") }
                ?: throw ProposalNotFoundByIdException(proposalId)

            val proposalType = proposal.type(checkResult)

            val aircraftTypeId = when (proposalType) {
                Proposal.Type.NEW -> createAircraftTypeFromProposalInTransaction(proposal)
                Proposal.Type.UPDATE -> updateAircraftTypeFromProposalInTransaction(proposal)
                Proposal.Type.DELETE -> softDeleteAircraftTypeFromProposalInTransaction(proposal)
                Proposal.Type.REJECTED -> null
                Proposal.Type.PENDING -> error("This is a bug, Type cannot be pending here. Bug 1 in AircraftTypeRepository.checkAircraftTypeProposal()")
            }

            markAircraftTypeProposalAsCheckedInTransaction(proposalId, checkResult, user, comment, aircraftTypeId)
        }
    }
}