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
     * @param user The user who wants this
     * @param offset The offset used for paging
     * @param limit The number of results on this page. Can be maximized by global configuration so not guaranteed.
     * Required authorization: [UserAuthorization.DISPLAY_OPEN_PROPOSALS]
     */
    suspend fun getOpenManufacturerProposals(user: User, limit: Int, offset: Long = 0): Result<PagedResult<ManufacturerProposal>>

    /**
     * Get all currently open AircraftTypeProposal.
     * @param user The user who wants this
     * @param offset The offset used for paging
     * @param limit The number of results on this page. Can be maximized by global configuration so not guaranteed.
     * Required authorization: [UserAuthorization.DISPLAY_OPEN_PROPOSALS]
     */
    suspend fun getOpenAircraftTypeProposals(user: User, limit: Int, offset: Long = 0): Result<PagedResult<AircraftTypeProposal>>

    /**
     * Get all currently open AircraftProposals.
     * @param user The user who wants this
     * @param offset The offset used for paging
     * @param limit The number of results on this page. Can be maximized by global configuration so not guaranteed.
     * Required authorization: [UserAuthorization.DISPLAY_OPEN_PROPOSALS]
     */
    suspend fun getOpenAircraftProposals(user: User, limit: Int, offset: Long = 0): Result<PagedResult<AircraftProposal>>

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