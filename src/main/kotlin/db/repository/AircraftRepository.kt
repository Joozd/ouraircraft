package org.ouraircraft.db.repository

import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.ouraircraft.auth.UserAuthorization
import org.ouraircraft.db.Database
import org.ouraircraft.db.exceptions.AlreadyCheckedException
import org.ouraircraft.db.exceptions.ProposalNotFoundByIdException
import org.ouraircraft.db.exceptions.TargetItemForProposalNotFoundByIdException
import org.ouraircraft.db.repository.ops.*
import org.ouraircraft.model.auth.User
import org.ouraircraft.model.data.Aircraft
import org.ouraircraft.model.data.AircraftProposal
import org.ouraircraft.model.data.CheckResult
import org.ouraircraft.model.data.Proposal
import org.ouraircraft.model.internal.Configuration

/**
 * Provides access to aircraft records in the database.
 *
 * All public operations execute within a single database transaction.
 * Retrieved aircraft include their associated aircraft type and manufacturer.
 *
 * All methods require a [User], which can be used for authorization checking.
 *
 * @property database Database used for repository operations.
 */
class AircraftRepository(val database: Database, val config: Configuration.DataRetrievalConfig) {

    /**
     * Finds aircraft whose registrations start with [prefix].
     *
     * The prefix is normalized before matching.
     *
     * @return Matching aircraft, or an empty list if none are found.
     */
    fun getAircraftByRegistrationPrefix(prefix: String, user: User): List<Aircraft> =
        transaction(database.exposed) {
            user.requireAuthorization(UserAuthorization.DISPLAY_AIRCRAFT)
            getAircraftByRegistrationPrefixInTransaction(prefix, maxMatches = config.maxPrefixMatches)
        }

    /**
     * Insert a new aircraft proposal into the database.
     * The user mentioned in the proposal needs to be checked/inserted by the service calling this;
     * this wil store 'as is'.
     */
    fun insertAircraftProposal(proposal: AircraftProposal, user: User) =
        transaction(database.exposed) {
            user.requireAuthorization(UserAuthorization.PROPOSE_AIRCRAFT)
            insertAircraftProposalInTransaction(proposal)
        }

    /**
     * Get all currently outstanding Aircraft Proposals
     */
    fun getAllOpenAircraftProposals(user: User): List<AircraftProposal> =
        transaction(database.exposed) {
            user.requireAuthorization(UserAuthorization.DISPLAY_PROPOSED_AIRCRAFT)
            getAllOpenAircraftProposalsInTransaction()
        }


    /**
     * Handles the result of reviewing an Aircraft proposal.
     *
     * In a single transaction, updates the proposal's review fields and,
     * if approved, creates, updates, or soft-deletes the aircraft.
     *
     * @param proposalId ID of the proposal being reviewed.
     * @param checkResult Review decision.
     * @param comment Optional explanation provided by the reviewer.
     * @param user User who performed the review.
     *
     * @throws org.ouraircraft.auth.exceptions.UserNotAuthorizedForThisException if [user] is not authorized for this action.
     * @throws AlreadyCheckedException if the proposal was already reviewed.
     * @throws ProposalNotFoundByIdException if the proposal does not exist.
     * @throws TargetItemForProposalNotFoundByIdException if the aircraft to update or soft-delete does not exist.
     */
    fun checkAircraftProposal(proposalId: Long, checkResult: CheckResult, comment: String?, user: User) {
        user.requireAuthorization(UserAuthorization.DECIDE_ON_PROPOSED_AIRCRAFT)

        transaction(database.exposed) {
            // check if this proposal (still) needs to be checked
            val proposal = getAircraftProposalByIdInTransaction(proposalId)
                ?.also { if (it.checkResult != null) throw AlreadyCheckedException("AircraftProposal $proposalId has already been checked.") }
                ?: throw ProposalNotFoundByIdException(proposalId)

            val proposalType = proposal.type(checkResult)

            val aircraftId = when (proposalType) {
                Proposal.Type.NEW -> createAircraftFromProposalInTransaction(proposal)
                Proposal.Type.UPDATE -> updateAircraftFromProposalInTransaction(proposal)
                Proposal.Type.DELETE -> softDeleteAircraftFromProposalInTransaction(proposal)
                Proposal.Type.REJECTED -> null
                Proposal.Type.PENDING -> error("This is a bug, Type cannot be pending here. Bug 1 in AircraftRepository.checkAircraftProposal()")
            }

            markAircraftProposalAsCheckedInTransaction(proposalId, checkResult, user, comment, aircraftId)
        }
    }
}
