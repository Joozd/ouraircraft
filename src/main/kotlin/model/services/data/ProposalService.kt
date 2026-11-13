package org.ouraircraft.model.services.data

import org.ouraircraft.auth.UserAuthorization
import org.ouraircraft.model.auth.User
import org.ouraircraft.model.data.AircraftProposal
import org.ouraircraft.model.data.AircraftTypeProposal
import org.ouraircraft.model.data.CheckResult
import org.ouraircraft.model.data.ManufacturerProposal
import org.ouraircraft.model.data.PagedResult

/**
 * This service takes care of all things involving Proposals, for Manufacturers, Aircraft Types and Aircraft:
 *  - Showing current proposals
 *  - Creating a new proposal
 *  - Checking a proposal and elevating it to live data
 *
 * This service is responsible for checking user authorizations.
 */
interface ProposalService {
    /**
     * Get all currently open ManufacturerProposals.
     * Required authorization: [UserAuthorization.DISPLAY_PROPOSED_MANUFACTURERS]
     */
    suspend fun getOpenManufacturerProposals(user: User, offset: Long = 0): Result<PagedResult<ManufacturerProposal>>

    /**
     * Get all currently open AircraftTypeProposal.
     * Required authorization: [UserAuthorization.DISPLAY_PROPOSED_AIRCRAFT_TYPES]
     */
    suspend fun getAircraftTypeProposals(user: User, offset: Long = 0): Result<PagedResult<AircraftTypeProposal>>

    /**
     * Get all currently open AircraftProposals.
     * Required authorization: [UserAuthorization.DISPLAY_PROPOSED_AIRCRAFT]
     */
    suspend fun getAircraftProposals(user: User, offset: Long = 0): Result<PagedResult<AircraftProposal>>

    /**
     * Propose a new Manufacturer.
     * Required authorization: [UserAuthorization.PROPOSE_MANUFACTURERS]
     */
    suspend fun proposeManufacturer(proposedManufacturer: ManufacturerProposal, user: User): Result<Unit>

    /**
     * Propose a new Aircraft Type
     * Required authorization: [UserAuthorization.PROPOSE_AIRCRAFT_TYPES]
     */
    suspend fun proposeAircraftType(proposedAircraftType: AircraftTypeProposal, user: User): Result<Unit>

    /**
     * Propose a new Aircraft.
     * Required authorization: [UserAuthorization.PROPOSE_AIRCRAFT]
     */
    suspend fun proposeAircraft(proposedAircraft: AircraftProposal, user: User): Result<Unit>

    /**
     * Handle the result of checking a proposed Manufacturer.
     * Required authorization: [UserAuthorization.DECIDE_ON_PROPOSED_MANUFACTURERS]
     */
    suspend fun handleManufactureProposalCheck(
        checkedProposalId: Long,
        checkResult: CheckResult,
        checkResultComment: String,
        user: User
    ): Result<Unit>

    /**
     * Handle the result of checking a proposed Aircraft Type.
     * Required authorization: [UserAuthorization.DECIDE_ON_PROPOSED_AIRCRAFT_TYPES]
     */
    suspend fun handleAircraftTypeProposalCheck(
        checkedProposalId: Long,
        checkResult: CheckResult,
        checkResultComment: String,
        user: User
    ): Result<Unit>

    /**
     * Handle the result of checking a proposed Aircraft.
     * Required authorization: [UserAuthorization.DECIDE_ON_PROPOSED_AIRCRAFT]
     */
    suspend fun handleAircraftProposalCheck(
        checkedProposalId: Long,
        checkResult: CheckResult,
        checkResultComment: String,
        user: User
    ): Result<Unit>
}