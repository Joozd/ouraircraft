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
        user: User
    ): Result<List<ManufacturerProposal>> = runCatchingAndMapToDomainException {
        user.requireAuthorization(UserAuthorization.DISPLAY_PROPOSED_MANUFACTURERS)
        manufacturerRepository.getAllOpenManufacturerProposals(user)
    }

    /**
     * Get all currently open AircraftTypeProposal.
     * Required authorization: [UserAuthorization.DISPLAY_PROPOSED_AIRCRAFT_TYPES]
     */
    override suspend fun getAircraftTypeProposals(
        user: User
    ): Result<List<AircraftTypeProposal>> = runCatchingAndMapToDomainException {
        user.requireAuthorization(UserAuthorization.DISPLAY_PROPOSED_AIRCRAFT_TYPES)
        aircraftTypeRepository.getAllOpenAircraftTypeProposals(user)
    }

    /**
     * Get all currently open AircraftProposals.
     * Required authorization: [UserAuthorization.DISPLAY_PROPOSED_AIRCRAFT]
     */
    override suspend fun getAircraftProposals(
        user: User
    ): Result<List<AircraftProposal>> = runCatchingAndMapToDomainException {
        user.requireAuthorization(UserAuthorization.DISPLAY_PROPOSED_AIRCRAFT)
        aircraftRepository.getAllOpenAircraftProposals(user)
    }

    /**
     * Propose a new Manufacturer.
     * Required authorization: [UserAuthorization.PROPOSE_MANUFACTURERS]
     */
    override suspend fun proposeManufacturer(
        proposedManufacturer: ManufacturerProposal,
        user: User
    ): Result<Unit> = runCatchingAndMapToDomainException {
        user.requireAuthorization(UserAuthorization.PROPOSE_MANUFACTURERS)
        manufacturerRepository.insertManufacturerProposal(proposedManufacturer, user)
    }

    /**
     * Propose a new Aircraft Type.
     * Required authorization: [UserAuthorization.PROPOSE_AIRCRAFT_TYPES]
     */
    override suspend fun proposeAircraftType(
        proposedAircraftType: AircraftTypeProposal,
        user: User
    ): Result<Unit> = runCatchingAndMapToDomainException {
        user.requireAuthorization(UserAuthorization.PROPOSE_AIRCRAFT_TYPES)
        aircraftTypeRepository.insertAircraftTypeProposal(proposedAircraftType, user)
    }

    /**
     * Propose a new Aircraft.
     * Required authorization: [UserAuthorization.PROPOSE_AIRCRAFT]
     */
    override suspend fun proposeAircraft(
        proposedAircraft: AircraftProposal,
        user: User
    ): Result<Unit> = runCatchingAndMapToDomainException {
        user.requireAuthorization(UserAuthorization.PROPOSE_AIRCRAFT)
        aircraftRepository.insertAircraftProposal(proposedAircraft, user)
    }

    /**
     * Handle the result of checking a proposed Manufacturer.
     * Required authorization: [UserAuthorization.DECIDE_ON_PROPOSED_MANUFACTURERS]
     */
    override suspend fun handleManufactureProposalCheck(
        checkedProposalId: Long,
        checkResult: CheckResult,
        checkResultComment: String,
        user: User
    ): Result<Unit> = runCatchingAndMapToDomainException {
        user.requireAuthorization(UserAuthorization.DECIDE_ON_PROPOSED_MANUFACTURERS)
        manufacturerRepository.checkManufacturerProposal(checkedProposalId, checkResult, checkResultComment.takeIf{it.isNotBlank()}, user)
    }

    /**
     * Handle the result of checking a proposed Aircraft Type.
     * Required authorization: [UserAuthorization.DECIDE_ON_PROPOSED_AIRCRAFT_TYPES]
     */
    override suspend fun handleAircraftTypeProposalCheck(
        checkedProposalId: Long,
        checkResult: CheckResult,
        checkResultComment: String,
        user: User
    ): Result<Unit> = runCatchingAndMapToDomainException {
        user.requireAuthorization(UserAuthorization.DECIDE_ON_PROPOSED_AIRCRAFT_TYPES)
        aircraftTypeRepository.checkAircraftTypeProposal(checkedProposalId, checkResult, checkResultComment.takeIf{it.isNotBlank()}, user)
    }

    /**
     * Handle the result of checking a proposed Aircraft.
     * Required authorization: [UserAuthorization.DECIDE_ON_PROPOSED_AIRCRAFT]
     */
    override suspend fun handleAircraftProposalCheck(
        checkedProposalId: Long,
        checkResult: CheckResult,
        checkResultComment: String,
        user: User
    ): Result<Unit> = runCatchingAndMapToDomainException {
        user.requireAuthorization(UserAuthorization.DECIDE_ON_PROPOSED_AIRCRAFT)
        aircraftRepository.checkAircraftProposal(checkedProposalId, checkResult, checkResultComment.takeIf{it.isNotBlank()}, user)
    }
}