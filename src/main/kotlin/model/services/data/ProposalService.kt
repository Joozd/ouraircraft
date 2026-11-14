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
    suspend fun getOpenAircraftTypeProposals(user: User, offset: Long = 0): Result<PagedResult<AircraftTypeProposal>>

    /**
     * Get all currently open AircraftProposals.
     * Required authorization: [UserAuthorization.DISPLAY_PROPOSED_AIRCRAFT]
     */
    suspend fun getOpenAircraftProposals(user: User, offset: Long = 0): Result<PagedResult<AircraftProposal>>

    /**
     * Propose a new Manufacturer.
     * Required authorization: [UserAuthorization.PROPOSE_MANUFACTURERS]
     */
    suspend fun proposeManufacturer(user: User, proposedManufacturer: ManufacturerProposal): Result<Unit>

    /**
     * Propose a new Aircraft Type
     * Required authorization: [UserAuthorization.PROPOSE_AIRCRAFT_TYPES]
     */
    suspend fun proposeAircraftType(user: User, proposedAircraftType: AircraftTypeProposal): Result<Unit>

    /**
     * Propose a new Aircraft.
     * Required authorization: [UserAuthorization.PROPOSE_AIRCRAFT]
     */
    suspend fun proposeAircraft(user: User, proposedAircraft: AircraftProposal): Result<Unit>

    /**
     * Handle the result of checking a proposed Manufacturer.
     * Required authorization: [UserAuthorization.DECIDE_ON_PROPOSED_MANUFACTURERS]
     */
    suspend fun handleManufacturerProposalCheck(
        user: User,
        checkedProposalId: Long,
        checkResult: CheckResult,
        checkResultComment: String?,
    ): Result<Unit>

    /**
     * Handle the result of checking a proposed Aircraft Type.
     * Required authorization: [UserAuthorization.DECIDE_ON_PROPOSED_AIRCRAFT_TYPES]
     */
    suspend fun handleAircraftTypeProposalCheck(
        user: User,
        checkedProposalId: Long,
        checkResult: CheckResult,
        checkResultComment: String?,
    ): Result<Unit>

    /**
     * Handle the result of checking a proposed Aircraft.
     * Required authorization: [UserAuthorization.DECIDE_ON_PROPOSED_AIRCRAFT]
     */
    suspend fun handleAircraftProposalCheck(
        user: User,
        checkedProposalId: Long,
        checkResult: CheckResult,
        checkResultComment: String?,
    ): Result<Unit>
}