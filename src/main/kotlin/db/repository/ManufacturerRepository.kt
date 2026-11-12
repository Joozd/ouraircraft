package org.ouraircraft.db.repository

import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.ouraircraft.auth.UserAuthorization
import org.ouraircraft.db.Database
import org.ouraircraft.db.exceptions.AlreadyCheckedException
import org.ouraircraft.db.exceptions.ProposalNotFoundByIdException
import org.ouraircraft.db.repository.ops.createManufacturerFromProposalInTransaction
import org.ouraircraft.db.repository.ops.softDeleteManufacturerFromProposalInTransaction
import org.ouraircraft.db.repository.ops.getAllOpenManufacturerProposalsInTransaction
import org.ouraircraft.db.repository.ops.getManufacturerProposalByIdInTransaction
import org.ouraircraft.db.repository.ops.getManufacturersByNamePrefixInTransaction
import org.ouraircraft.db.repository.ops.insertManufacturerProposalInTransaction
import org.ouraircraft.db.repository.ops.markManufacturerProposalAsCheckedInTransaction
import org.ouraircraft.db.repository.ops.updateManufacturerFromProposalInTransaction
import org.ouraircraft.model.auth.User
import org.ouraircraft.model.data.CheckResult
import org.ouraircraft.model.data.Manufacturer
import org.ouraircraft.model.data.ManufacturerProposal
import org.ouraircraft.model.data.Proposal

class ManufacturerRepository(val database: Database) {
    /**
     * Finds manufacturers whose names start with [prefix].
     *
     * The prefix is normalized before matching.
     *
     * @return Matching manufacturers, or an empty list if none are found.
     */
    fun getManufacturersByNamePrefix(prefix: String, user: User): List<Manufacturer> =
        transaction(database.exposed) {
            user.requireAuthorization(UserAuthorization.DISPLAY_MANUFACTURER)
            getManufacturersByNamePrefixInTransaction(prefix)
        }


    /**
     * Insert a new Manufacturer proposal into the database.
     * The user mentioned in the proposal needs to be checked/inserted by the service calling this;
     * this wil store 'as is'.
     */
    fun insertManufacturerProposal(proposal: ManufacturerProposal, user: User) =
        transaction(database.exposed) {
            user.requireAuthorization(UserAuthorization.PROPOSE_MANUFACTURER)

            insertManufacturerProposalInTransaction(proposal)
        }


    /**
     * Get all currently outstanding Manufacturer Proposals
     */
    fun getAllOpenManufacturerProposals(user: User): List<ManufacturerProposal> =
        transaction(database.exposed) {
            user.requireAuthorization(UserAuthorization.DISPLAY_PROPOSED_MANUFACTURERS)
            getAllOpenManufacturerProposalsInTransaction()
        }

    /**
     * Handle the result of checking a Manufacturer Proposal.
     * This will, in a single transaction, update the Proposal record,
     * and, if the result is [CheckResult.APPROVED], create a Manufacturer record.
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
    fun checkManufacturerProposal(proposalId: Long, checkResult: CheckResult, comment: String?, user: User) {
        user.requireAuthorization(UserAuthorization.DECIDE_ON_PROPOSED_MANUFACTURER)

        transaction(database.exposed) {
            // check if this proposal (still) needs to be checked
            val proposal = getManufacturerProposalByIdInTransaction(proposalId)
                ?.also { if (it.checkResult != null) throw AlreadyCheckedException("ManufacturerProposal $proposalId has already been checked.") }
                ?: throw ProposalNotFoundByIdException(proposalId)

            val proposalType = proposal.type(checkResult)

            val manufacturerId = when(proposalType){
                Proposal.Type.NEW -> createManufacturerFromProposalInTransaction(proposal)
                Proposal.Type.UPDATE -> updateManufacturerFromProposalInTransaction(proposal)
                Proposal.Type.DELETE -> softDeleteManufacturerFromProposalInTransaction(proposal)
                Proposal.Type.REJECTED -> null
                Proposal.Type.PENDING -> error("This is a bug, Type cannot be pending here. Bug 1 in ManufacturerRepository.checkManufacturerProposal()")
            }

            markManufacturerProposalAsCheckedInTransaction(proposalId, checkResult, user, comment, manufacturerId)
        }
    }
}