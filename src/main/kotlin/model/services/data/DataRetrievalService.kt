package org.ouraircraft.model.services.data

import org.ouraircraft.auth.UserAuthorization
import org.ouraircraft.model.auth.User
import org.ouraircraft.model.data.Aircraft
import org.ouraircraft.model.data.AircraftType
import org.ouraircraft.model.data.Manufacturer
import org.ouraircraft.model.data.PagedResult

/**
 * Service for retrieving Manufacturer, Aircraft Type, and Aircraft data.
 *
 * This service is responsible for checking user authorizations.
 */
interface DataRetrievalService {
    /**
     * Gets all Manufacturers whose name starts with [namePrefix]
     * Required authorization: [UserAuthorization.DISPLAY_MANUFACTURERS]
     */
    suspend fun getManufacturersByNamePrefix(namePrefix: String, user: User): Result<PagedResult<Manufacturer>>

    /**
     * Gets all Aircraft Types whose family starts with [familyPrefix]
     * Required authorization: [UserAuthorization.DISPLAY_AIRCRAFT_TYPES]
     */
    suspend fun getAircraftTypesByFamilyPrefix(familyPrefix: String, user: User): Result<PagedResult<AircraftType>>

    /**
     * Gets all Aircraft Types whose ICAO designator starts with [designatorPrefix]
     * Required authorization: [UserAuthorization.DISPLAY_AIRCRAFT_TYPES]
     */
    suspend fun getAircraftTypesByDesignatorPrefix(designatorPrefix: String, user: User): Result<PagedResult<AircraftType>>

    /**
     * Gets all Aircraft whose registration designator starts with [registrationPrefix]
     * Required authorization: [UserAuthorization.DISPLAY_AIRCRAFT]
     */
    suspend fun getAircraftByRegistrationPrefix(registrationPrefix: String, user: User): Result<PagedResult<Aircraft>>
}