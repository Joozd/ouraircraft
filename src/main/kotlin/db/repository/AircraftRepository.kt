package org.ouraircraft.db.repository

import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.ouraircraft.auth.UserAuthorization
import org.ouraircraft.db.Database
import org.ouraircraft.db.exceptions.AlreadyCheckedException
import org.ouraircraft.db.exceptions.ProposalNotFoundByIdException
import org.ouraircraft.db.repository.ops.*
import org.ouraircraft.model.auth.User
import org.ouraircraft.model.data.Aircraft
import org.ouraircraft.model.data.AircraftProposal
import org.ouraircraft.model.data.CheckResult

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
class AircraftRepository(val database: Database) {

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
            getAircraftByRegistrationPrefixInTransaction(prefix)
        }


    /**
     * Finds an aircraft by its unique database [id].
     *
     * @return The aircraft, or null if no matching record exists.
     */
    fun getAircraftById(id: Long, user: User): Aircraft? =
        transaction(database.exposed) {
            user.requireAuthorization(UserAuthorization.DISPLAY_AIRCRAFT)
            findAircraftByIdInTransaction(id)
        }

    /**
     * Insert a new aircraft proposal into the database.
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
     * Handle the result of checking an Aircraft Proposal.
     * This will, in a single transaction, update the Proposal record,
     * and, if the result is [CheckResult.APPROVED], create or an Aircraft record.
     *
     * @param proposalId The ID of the Proposal that is being updated
     * @param checkResult The result of the check
     * @param user The User that did this check
     * @param comment Any comments the checker might want to add.
     *
     * @throws org.ouraircraft.auth.exceptions.UserNotAuthorizedForThisException if [user] is not authorized for this action.
     * @throws AlreadyCheckedException if the proposal was already checked
     * @throws ProposalNotFoundByIdException if the proposal does not exist in the DB
     *
     */
    fun checkAircraftProposal(proposalId: Long, checkResult: CheckResult, comment: String?, user: User) {
        user.requireAuthorization(UserAuthorization.DECIDE_ON_PROPOSED_AIRCRAFT)

        transaction(database.exposed) {
            // check if this proposal (still) needs to be checked
            val proposal = getAircraftProposalByIdInTransaction(proposalId)
                ?.also{ if(it.checkResult != null) throw AlreadyCheckedException("AircraftProposal $proposalId has already been checked.") }
                ?: throw ProposalNotFoundByIdException(proposalId)

            // if approved, make the Aircraft, else leave at null.
            val aircraftId =
                if(checkResult == CheckResult.APPROVED) createAircraftFromProposalInTransaction(proposal)
                else null

            markAircraftProposalAsCheckedInTransaction(proposalId, checkResult, user, comment, aircraftId)
        }
    }
}
