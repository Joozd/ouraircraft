package org.ouraircraft.db.service

import org.ouraircraft.db.exceptions.UserNotFoundByIDException
import org.ouraircraft.model.services.utils.runCatchingAndMapToDomainException
import org.ouraircraft.model.auth.PasskeyData
import org.ouraircraft.model.services.users.PasskeyService
import org.ouraircraft.model.auth.valueclasses.PasskeyCredentialId
import org.ouraircraft.db.repository.AuthRepository
import org.ouraircraft.db.repository.UserRepository
import kotlin.uuid.Uuid

internal class PasskeyServiceImpl(val authRepository: AuthRepository, val userRepository: UserRepository): PasskeyService {
    /**
     * Get [PasskeyCredentialId]s for all valid passkey logins for user with identity
     */
    override fun getPasskeyCredentialIdsForUserId(userId: Uuid): Result<List<PasskeyCredentialId>> =
        runCatchingAndMapToDomainException {
            authRepository.getPasskeyCredentialIdsForUser(userId)
        }

    /**
     * Get the [Uuid] for the user with screenName [screenName]
     * @throws org.ouraircraft.db.exceptions.UserNotFoundByScreenNameException If no user exists with the given screenName.
     */
    override fun getIdFromScreenName(screenName: String): Result<Uuid> =
        runCatchingAndMapToDomainException {
            userRepository.findUserByScreenName(screenName).id
        }

    /**
     * Get the [Uuid] for the user with id [userId]
     * @throws UserNotFoundByIDException If no user exists with the given ID.
     */
    override fun getScreenNameFromId(userId: Uuid): Result<String> =
        runCatchingAndMapToDomainException {
            userRepository.getUserById(userId).screenName
        }


    /**
    * Get [PasskeyCredentialId]s for all valid passkey logins for user with identity
    */
    override fun getPasskeyCredentialIdsForScreenName(screenName: String): Result<List<PasskeyCredentialId>> =
        runCatchingAndMapToDomainException {
            authRepository.getPasskeyCredentialIdsForUser(screenName)
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

    /**
     * Store passkey data
     */
    override fun storePasskeyData(passkeyData: PasskeyData) {
        authRepository.writePasskeyData(passkeyData)
    }

    override fun updatePasskeyData(passkeyData: PasskeyData){
        authRepository.updatePasskeyData(passkeyData)
    }
}