package org.ouraircraft.db.service

import org.ouraircraft.auth.UserAuthorization
import org.ouraircraft.db.repository.AircraftRepository
import org.ouraircraft.db.repository.AircraftTypeRepository
import org.ouraircraft.db.repository.ManufacturerRepository
import org.ouraircraft.model.auth.User
import org.ouraircraft.model.data.Aircraft
import org.ouraircraft.model.data.AircraftType
import org.ouraircraft.model.data.Manufacturer
import org.ouraircraft.model.data.PagedResult
import org.ouraircraft.model.services.data.DataRetrievalService
import org.ouraircraft.model.services.utils.runCatchingAndMapToDomainException

class DataRetrievalServiceImpl(
    private val manufacturerRepository: ManufacturerRepository,
    private val aircraftTypeRepository: AircraftTypeRepository,
    private val aircraftRepository: AircraftRepository
): DataRetrievalService {
    /**
     * Gets all Manufacturers whose name starts with [namePrefix]
     * If query is too short, this returns no results.
     * Required authorization: [UserAuthorization.DISPLAY_MANUFACTURERS]
     */
    override suspend fun getManufacturersByNamePrefix(
        namePrefix: String,
        user: User,
        limit: Int,
        offset: Long
    ): Result<PagedResult<Manufacturer>> = runCatchingAndMapToDomainException {
        user.requireAuthorization(UserAuthorization.DISPLAY_MANUFACTURERS)

        if(namePrefix.trim().length < 2) return@runCatchingAndMapToDomainException PagedResult.empty()

        manufacturerRepository.getManufacturersByNamePrefix(user, namePrefix, limit, offset)
    }

    /**
     * Gets all Aircraft Types whose family starts with [familyPrefix]
     * If query is too short, this returns no results.
     * Required authorization: [UserAuthorization.DISPLAY_AIRCRAFT_TYPES]
     */
    override suspend fun getAircraftTypesByFamilyPrefix(
        familyPrefix: String,
        user: User,
        limit: Int,
        offset: Long
    ): Result<PagedResult<AircraftType>> = runCatchingAndMapToDomainException {
        user.requireAuthorization(UserAuthorization.DISPLAY_AIRCRAFT_TYPES)

        if(familyPrefix.trim().length < 2) return@runCatchingAndMapToDomainException PagedResult.empty()

        aircraftTypeRepository.getAircraftTypesByFamilyPrefix(user, familyPrefix, limit, offset)
    }

    /**
     * Gets all Aircraft Types whose ICAO designator starts with [designatorPrefix]
     * If query is too short, this returns no results.
     * Required authorization: [UserAuthorization.DISPLAY_AIRCRAFT_TYPES]
     */
    override suspend fun getAircraftTypesByDesignatorPrefix(
        designatorPrefix: String,
        user: User,
        limit: Int,
        offset: Long
    ): Result<PagedResult<AircraftType>> = runCatchingAndMapToDomainException {
        user.requireAuthorization(UserAuthorization.DISPLAY_AIRCRAFT_TYPES)

        if(designatorPrefix.trim().length < 2) return@runCatchingAndMapToDomainException PagedResult.empty()
        aircraftTypeRepository.getAircraftTypesByDesignatorPrefix(user, designatorPrefix, limit, offset)
    }

    /**
     * Gets all Aircraft whose registration designator starts with [registrationPrefix]
     * If query is too short, this returns no results.
     * Required authorization: [UserAuthorization.DISPLAY_AIRCRAFT]
     */
    override suspend fun getAircraftByRegistrationPrefix(
        registrationPrefix: String,
        user: User,
        limit: Int,
        offset: Long
    ): Result<PagedResult<Aircraft>> = runCatchingAndMapToDomainException {
        user.requireAuthorization(UserAuthorization.DISPLAY_AIRCRAFT)

        if(registrationPrefix.trim().length < 2) return@runCatchingAndMapToDomainException PagedResult.empty()
        aircraftRepository.getAircraftByRegistrationPrefix(user, registrationPrefix, limit, offset)
    }
}