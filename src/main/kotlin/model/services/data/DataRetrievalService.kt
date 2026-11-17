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
     * Gets the Manufacturer with id [id], or null if that doesn't exist
     * Required authorization: [UserAuthorization.DISPLAY_MANUFACTURERS]
     */
    suspend fun getManufacturerById(user: User, id: Long): Result<Manufacturer?>

    /**
     * Gets all Manufacturers whose name starts with [namePrefix]
     * Required authorization: [UserAuthorization.DISPLAY_MANUFACTURERS]
     */
    suspend fun getManufacturersByNamePrefix(user: User, namePrefix: String, limit: Int, offset: Long = 0): Result<PagedResult<Manufacturer>>

    /**
     * Gets the Aircraft Type with id [id], or null if that doesn't exist
     * Required authorization: [UserAuthorization.DISPLAY_AIRCRAFT_TYPES]
     */
    suspend fun getAircraftTypeById(user: User, id: Long): Result<AircraftType?>

    /**
     * Gets all Aircraft Types whose family starts with [familyPrefix]
     * Required authorization: [UserAuthorization.DISPLAY_AIRCRAFT_TYPES]
     */
    suspend fun getAircraftTypesByFamilyPrefix(user: User, familyPrefix: String, limit: Int, offset: Long = 0): Result<PagedResult<AircraftType>>

    /**
     * Gets all Aircraft Types whose ICAO designator starts with [designatorPrefix]
     * Required authorization: [UserAuthorization.DISPLAY_AIRCRAFT_TYPES]
     */
    suspend fun getAircraftTypesByDesignatorPrefix(user: User, designatorPrefix: String, limit: Int, offset: Long = 0): Result<PagedResult<AircraftType>>

    /**
     * Gets the Aircraft with id [id], or null if that doesn't exist
     * Required authorization: [UserAuthorization.DISPLAY_AIRCRAFT]
     */
    suspend fun getAircraftById(user: User, id: Long): Result<Aircraft?>

    /**
     * Gets all Aircraft whose registration designator starts with [registrationPrefix]
     * Required authorization: [UserAuthorization.DISPLAY_AIRCRAFT]
     */
    suspend fun getAircraftByRegistrationPrefix(user: User, registrationPrefix: String, limit: Int, offset: Long = 0): Result<PagedResult<Aircraft>>


    /**
     * Gets aircraft type by combination of designator and registration.
     * Null values count as all,
     * if both values are null, or queries are too short, this will return an empty page.
     */
    suspend fun getAircraftTypesByDesignatorOrRegistration(
        designatorPrefix: String?,
        familyPrefix: String?,
        user: User,
        limit: Int,
        offset: Long
    ): Result<PagedResult<AircraftType>>
}