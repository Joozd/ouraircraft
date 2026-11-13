package org.ouraircraft.db.service

import org.ouraircraft.auth.UserAuthorization
import org.ouraircraft.db.repository.AircraftRepository
import org.ouraircraft.db.repository.AircraftTypeRepository
import org.ouraircraft.db.repository.ManufacturerRepository
import org.ouraircraft.model.auth.User
import org.ouraircraft.model.data.Aircraft
import org.ouraircraft.model.data.AircraftType
import org.ouraircraft.model.data.Manufacturer
import org.ouraircraft.model.services.data.DataRetrievalService
import org.ouraircraft.model.services.utils.runCatchingAndMapToDomainException

class DataRetrievalServiceImpl(
    private val manufacturerRepository: ManufacturerRepository,
    private val aircraftTypeRepository: AircraftTypeRepository,
    private val aircraftRepository: AircraftRepository
): DataRetrievalService {
    /**
     * Gets all Manufacturers whose name starts with [namePrefix]
     * Required authorization: [UserAuthorization.DISPLAY_MANUFACTURERS]
     */
    override suspend fun getManufacturersByNamePrefix(
        namePrefix: String,
        user: User
    ): Result<List<Manufacturer>> = runCatchingAndMapToDomainException {
        user.requireAuthorization(UserAuthorization.DISPLAY_MANUFACTURERS)
        manufacturerRepository.getManufacturersByNamePrefix(namePrefix, user)
    }

    /**
     * Gets all Aircraft Types whose family starts with [familyPrefix]
     * Required authorization: [UserAuthorization.DISPLAY_AIRCRAFT_TYPES]
     */
    override suspend fun getAircraftTypesByFamilyPrefix(
        familyPrefix: String,
        user: User
    ): Result<List<AircraftType>> = runCatchingAndMapToDomainException {
        user.requireAuthorization(UserAuthorization.DISPLAY_AIRCRAFT_TYPES)
        aircraftTypeRepository.getAircraftTypesByFamilyPrefix(familyPrefix, user)
    }

    /**
     * Gets all Aircraft Types whose ICAO designator starts with [designatorPrefix]
     * Required authorization: [UserAuthorization.DISPLAY_AIRCRAFT_TYPES]
     */
    override suspend fun getAircraftTypesByDesignatorPrefix(
        designatorPrefix: String,
        user: User
    ): Result<List<AircraftType>> = runCatchingAndMapToDomainException {
        user.requireAuthorization(UserAuthorization.DISPLAY_AIRCRAFT_TYPES)
        aircraftTypeRepository.getAircraftTypesByDesignatorPrefix(designatorPrefix, user)
    }

    /**
     * Gets all Aircraft whose registration designator starts with [registrationPrefix]
     * Required authorization: [UserAuthorization.DISPLAY_AIRCRAFT]
     */
    override suspend fun getAircraftByRegistrationPrefix(
        registrationPrefix: String,
        user: User
    ): Result<List<Aircraft>> = runCatchingAndMapToDomainException {
        user.requireAuthorization(UserAuthorization.DISPLAY_AIRCRAFT)
        aircraftRepository.getAircraftByRegistrationPrefix(registrationPrefix, user)
    }
}