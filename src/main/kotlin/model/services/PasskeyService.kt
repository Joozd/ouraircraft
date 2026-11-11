package org.ouraircraft.model.services

import org.ouraircraft.model.auth.PasskeyData
import org.ouraircraft.model.auth.valueclasses.PasskeyCredentialId
import kotlin.uuid.Uuid

interface PasskeyService {
    /**
     * Get [PasskeyCredentialId]s for all valid passkey logins for user with identity
     */
    fun getPasskeyCredentialIdsForUserId(userId: Uuid): Result<List<PasskeyCredentialId>>

    /**
     * Get the [Uuid] for the user with screenName [screenName]
     */
    fun getIdFromScreenName(screenName: String): Result<Uuid>


    /**
     * Get the [Uuid] for the user with id [userId]
     */
    fun getScreenNameFromId(userId: Uuid): Result<String>

     /**
     * Get [PasskeyCredentialId]s for all valid passkey logins for user with identity
     */
    fun getPasskeyCredentialIdsForScreenName(screenName: String): Result<List<PasskeyCredentialId>>

    /**
     * Get [PasskeyCredentialId] for the [Uuid]/[PasskeyCredentialId] combination, or null if none found.
     */
    fun getPasskeyData(userId: Uuid, credentialId: PasskeyCredentialId): Result<PasskeyData?>

    /**
     * Get [PasskeyCredentialId] for the [PasskeyCredentialId], or null if none found.
     */
    fun getPasskeyData(credentialId: PasskeyCredentialId): Result<PasskeyData?>


    /**
     * Store passkey data for user
     */
    fun storePasskeyData(passkeyData: PasskeyData)

    /**
     * Update passkey data for user.
     */
    fun updatePasskeyData(passkeyData: PasskeyData)
}