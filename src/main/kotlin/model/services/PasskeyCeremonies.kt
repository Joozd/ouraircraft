package org.ouraircraft.model.services

import org.ouraircraft.model.auth.PasskeyData
import org.ouraircraft.model.dto.PasskeyRegistrationResponse
import org.ouraircraft.model.dto.PasskeyRequest
import org.ouraircraft.model.dto.PasskeyResponse
import kotlin.uuid.Uuid

/**
 * Passkey Ceremonies produce formed JSON strings to pass to frontend.
 * As far as the application is concerned, this is an opaque process that does not need to be understood farther than
 * "data is sent to frontend to use in ceremony x", "data for ceremony x is sent to backend"
 */
interface PasskeyCeremonies {
    /**
     * Make a Json String to pass to the frontend, so it can generate a passkey,
     * and send a response to the appropriate API Route.
     */
    suspend fun makePasskeyCreationOptionsJsonString(userId: Uuid): String

    /**
     * Handle the response to the data created by [makePasskeyCreationOptionsJsonString].
     * This step persists the passkey data to local storage.
     */
    suspend fun handlePasskeyRegistrationResponse(userId: Uuid, response: PasskeyRegistrationResponse): PasskeyData

    /**
     * Make a [PasskeyRequest] containing a Json String to pass to the frontend, so it can send a response to the appropriate API Route.
     */
    suspend fun generateAuthnRequestJson(): PasskeyRequest


    /**
     * handle the response for a response given to a login attempt
     * @throws org.ouraircraft.auth.exceptions.PasskeyCeremonyNotFoundException when there is no information on how the request started.
     * @throws org.ouraircraft.auth.exceptions.BadCredentialsException if login failed because the provided credentials are not accepted.
     * @return the [PasskeyData] with the verified login details.
     */
    suspend fun handleAuthnRequestResponse(response: PasskeyResponse): PasskeyData
}