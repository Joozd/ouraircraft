package org.ouraircraft.services.impl

import org.ouraircraft.db.exceptions.runCatchingAndMapToDomainException
import org.ouraircraft.model.PasskeyData
import org.ouraircraft.model.valueclasses.PasskeyCredentialId
import org.ouraircraft.repository.AuthRepository
import org.ouraircraft.services.PasskeyService
import kotlin.uuid.Uuid

internal class PasskeyServiceImpl(val authRepository: AuthRepository): PasskeyService {
    /**
     * Get [PasskeyCredentialId]s for all valid passkey logins for user with identity
     */
    override fun getPasskeyCredentialIdsForUserId(userId: Uuid): Result<List<PasskeyCredentialId>> =
        runCatchingAndMapToDomainException {
            authRepository.getPasskeyCredentialIdsForUser(userId)
        }


    /**
     * Get [PasskeyCredentialId]s for all valid passkey logins for user with identity
     */
    override fun getPasskeyData(
        userId: Uuid,
        credentialId: PasskeyCredentialId
    ): Result<PasskeyData?> = runCatchingAndMapToDomainException {
        authRepository.getPasskeyData(credentialId)
            ?.takeIf{ it.userId == userId }
    }

    /**
     * Get [PasskeyCredentialId] for the [PasskeyCredentialId], or null if none found.
     */
    override fun getPasskeyData(
        credentialId: PasskeyCredentialId
    ): Result<PasskeyData?> = runCatchingAndMapToDomainException {
        authRepository.getPasskeyData(credentialId)
    }
}