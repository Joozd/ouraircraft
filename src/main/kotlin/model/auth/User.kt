package org.ouraircraft.model.auth

import org.ouraircraft.auth.UserAuthorization
import org.ouraircraft.auth.exceptions.UserNotAuthorizedForThisException
import org.ouraircraft.crypt.EncryptedAesGcmText
import kotlin.time.Instant
import kotlin.uuid.Uuid

data class User(
    val id: Uuid,
    val screenName: String,
    val encryptedEmail: EncryptedAesGcmText,
    val createdAt: Instant,
    val authorizations: Set<UserAuthorization>
) {
    /**
     * Checks if the user has authorization [authorization]
     * @throws [UserNotAuthorizedForThisException] if the user is not authorized.
     */
    fun requireAuthorization(authorization: UserAuthorization) {
        if (!authorizations.contains(authorization))
            throw UserNotAuthorizedForThisException("This action requires authorization $authorization, which the User does not have.")
    }
}