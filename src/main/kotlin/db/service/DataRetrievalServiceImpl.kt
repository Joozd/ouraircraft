package org.ouraircraft.db.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
        withContext(Dispatchers.IO) {
            aircraftTypeRepository.getAircraftTypesByDesignatorPrefix(user, designatorPrefix, limit, offset)
        }
    }

    /**
     * Gets aircraft type by combination of designator and registration.
     * Null values count as all,
     * if both values are null, or queries are too short, this will return an empty page.
     */
    override suspend fun getAircraftTypesByDesignatorOrRegistration(
        designatorPrefix: String?,
        familyPrefix: String?,
        user: User,
        limit: Int,
        offset: Long
    ): Result<PagedResult<AircraftType>> = runCatchingAndMapToDomainException {
        withContext(Dispatchers.IO) {
            when {
                designatorPrefix == null && familyPrefix == null -> PagedResult.empty()
                designatorPrefix == null -> getAircraftTypesByFamilyPrefix(familyPrefix!!, user, limit, offset).getOrThrow()
                familyPrefix == null -> getAircraftTypesByDesignatorPrefix(designatorPrefix, user, limit, offset).getOrThrow()
                else -> getAircraftTypesByDesignatorAndRegistration(designatorPrefix, familyPrefix, user, limit, offset)
            }
        }
    }

    private suspend fun getAircraftTypesByDesignatorAndRegistration(
        designatorPrefix: String,
        familyPrefix: String,
        user: User,
        limit: Int,
        offset: Long
    ): PagedResult<AircraftType> = withContext(Dispatchers.IO) {
        user.requireAuthorization(UserAuthorization.DISPLAY_AIRCRAFT_TYPES)

        if(designatorPrefix.trim().length < 2 && familyPrefix.trim().length < 2) return@withContext PagedResult.empty() // quick exit
        aircraftTypeRepository.getAircraftTypesByDesignatorPrefixAndFamilyPrefix(user, designatorPrefix, familyPrefix, limit, offset)
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
        withContext(Dispatchers.IO) {
            aircraftRepository.getAircraftByRegistrationPrefix(user, registrationPrefix, limit, offset)
        }
    }
}