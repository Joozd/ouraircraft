package org.ouraircraft.db.service

import org.ouraircraft.auth.UserAuthorization
import org.ouraircraft.db.repository.AircraftRepository
import org.ouraircraft.db.repository.AircraftTypeRepository
import org.ouraircraft.db.repository.ManufacturerRepository
import org.ouraircraft.model.auth.User
import org.ouraircraft.model.data.AircraftProposal
import org.ouraircraft.model.data.AircraftTypeProposal
import org.ouraircraft.model.data.CheckResult
import org.ouraircraft.model.data.ManufacturerProposal
import org.ouraircraft.model.data.PagedResult
import org.ouraircraft.model.services.data.ProposalService
import org.ouraircraft.model.services.utils.runCatchingAndMapToDomainException

class ProposalServiceImpl(
    private val manufacturerRepository: ManufacturerRepository,
    private val aircraftTypeRepository: AircraftTypeRepository,
    private val aircraftRepository: AircraftRepository
) : ProposalService {
    /**
     * Get all currently open ManufacturerProposals.
     * Required authorization: [UserAuthorization.DISPLAY_PROPOSED_MANUFACTURERS]
     */
    override suspend fun getOpenManufacturerProposals(
        user: User,
        offset: Long
    ): Result<PagedResult<ManufacturerProposal>> = runCatchingAndMapToDomainException {
        user.requireAuthorization(UserAuthorization.DISPLAY_PROPOSED_MANUFACTURERS)
        manufacturerRepository.getOpenManufacturerProposals(user, offset)
    }

    /**
     * Get all currently open AircraftTypeProposal.
     * Required authorization: [UserAuthorization.DISPLAY_PROPOSED_AIRCRAFT_TYPES]
     */
    override suspend fun getOpenAircraftTypeProposals(
        user: User,
        offset: Long
    ): Result<PagedResult<AircraftTypeProposal>> = runCatchingAndMapToDomainException {
        user.requireAuthorization(UserAuthorization.DISPLAY_PROPOSED_AIRCRAFT_TYPES)
        aircraftTypeRepository.getOpenAircraftTypeProposals(user, offset)
    }

    /**
     * Get all currently open AircraftProposals.
     * Required authorization: [UserAuthorization.DISPLAY_PROPOSED_AIRCRAFT]
     */
    override suspend fun getOpenAircraftProposals(
        user: User,
        offset: Long
    ): Result<PagedResult<AircraftProposal>> = runCatchingAndMapToDomainException {
        user.requireAuthorization(UserAuthorization.DISPLAY_PROPOSED_AIRCRAFT)
        aircraftRepository.getOpenAircraftProposals(user, offset)
    }

    /**
     * Propose a new Manufacturer.
     * Required authorization: [UserAuthorization.PROPOSE_MANUFACTURERS]
     */
    override suspend fun proposeManufacturer(
        user: User,
        proposedManufacturer: ManufacturerProposal,
    ): Result<Unit> = runCatchingAndMapToDomainException {
        user.requireAuthorization(UserAuthorization.PROPOSE_MANUFACTURERS)
        manufacturerRepository.insertManufacturerProposal(user, proposedManufacturer)
    }

    /**
     * Propose a new Aircraft Type.
     * Required authorization: [UserAuthorization.PROPOSE_AIRCRAFT_TYPES]
     */
    override suspend fun proposeAircraftType(
        user: User,
        proposedAircraftType: AircraftTypeProposal,
    ): Result<Unit> = runCatchingAndMapToDomainException {
        user.requireAuthorization(UserAuthorization.PROPOSE_AIRCRAFT_TYPES)
        aircraftTypeRepository.insertAircraftTypeProposal(user, proposedAircraftType)
    }

    /**
     * Propose a new Aircraft.
     * Required authorization: [UserAuthorization.PROPOSE_AIRCRAFT]
     */
    override suspend fun proposeAircraft(
        user: User,
        proposedAircraft: AircraftProposal,
    ): Result<Unit> = runCatchingAndMapToDomainException {
        user.requireAuthorization(UserAuthorization.PROPOSE_AIRCRAFT)
        aircraftRepository.insertAircraftProposal(proposedAircraft, user)
    }

    /**
     * Handle the result of checking a proposed Manufacturer.
     * Required authorization: [UserAuthorization.DECIDE_ON_PROPOSED_MANUFACTURERS]
     */
    override suspend fun handleManufacturerProposalCheck(
        user: User,
        checkedProposalId: Long,
        checkResult: CheckResult,
        checkResultComment: String?,
    ): Result<Unit> = runCatchingAndMapToDomainException {
        user.requireAuthorization(UserAuthorization.DECIDE_ON_PROPOSED_MANUFACTURERS)
        manufacturerRepository.checkManufacturerProposal(user, checkedProposalId, checkResult, checkResultComment?.takeIf{it.isNotBlank()})
    }

    /**
     * Handle the result of checking a proposed Aircraft Type.
     * Required authorization: [UserAuthorization.DECIDE_ON_PROPOSED_AIRCRAFT_TYPES]
     */
    override suspend fun handleAircraftTypeProposalCheck(
        user: User,
        checkedProposalId: Long,
        checkResult: CheckResult,
        checkResultComment: String?,
    ): Result<Unit> = runCatchingAndMapToDomainException {
        user.requireAuthorization(UserAuthorization.DECIDE_ON_PROPOSED_AIRCRAFT_TYPES)
        aircraftTypeRepository.checkAircraftTypeProposal(user, checkedProposalId, checkResult, checkResultComment?.takeIf{it.isNotBlank()})
    }

    /**
     * Handle the result of checking a proposed Aircraft.
     * Required authorization: [UserAuthorization.DECIDE_ON_PROPOSED_AIRCRAFT]
     */
    override suspend fun handleAircraftProposalCheck(
        user: User,
        checkedProposalId: Long,
        checkResult: CheckResult,
        checkResultComment: String?,
    ): Result<Unit> = runCatchingAndMapToDomainException {
        user.requireAuthorization(UserAuthorization.DECIDE_ON_PROPOSED_AIRCRAFT)
        aircraftRepository.checkAircraftProposal(checkedProposalId, checkResult, checkResultComment?.takeIf{it.isNotBlank()}, user)
    }
}