package org.ouraircraft.services

import org.ouraircraft.model.PasskeyData
import org.ouraircraft.model.valueclasses.PasskeyCredentialId
import kotlin.uuid.Uuid

interface PasskeyService {
    /**
     * Get [PasskeyCredentialId]s for all valid passkey logins for user with identity
     */
    fun getPasskeyCredentialIdsForUserId(userId: Uuid): Result<List<PasskeyCredentialId>>

    /**
     * Get [PasskeyCredentialId] for the [Uuid]/[PasskeyCredentialId] combination, or null if none found.
     */
    fun getPasskeyData(userId: Uuid, credentialId: PasskeyCredentialId): Result<PasskeyData?>

    /**
     * Get [PasskeyCredentialId] for the [PasskeyCredentialId], or null if none found.
     */
    fun getPasskeyData(credentialId: PasskeyCredentialId): Result<PasskeyData?>
}