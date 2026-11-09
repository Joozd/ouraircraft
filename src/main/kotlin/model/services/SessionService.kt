package org.ouraircraft.services

import org.ouraircraft.auth.PasskeyAddingSessionToken
import org.ouraircraft.auth.RecoveryToken
import org.ouraircraft.model.dto.sessions.PasskeyAddingSessionData

/**
 * Provides operations for keeping track of login sessions
 */
interface SessionService {
    /**
     * Creates a PasskeyAddingSession.
     *
     * @param recoveryToken A [RecoveryToken] that points to a user/recoverytoken combination.
     *  if the RecoveryToken matches a user, a new PasskeyAddingSession is created for that user.
     *
     * @return The [PasskeyAddingSessionData] paired to the [PasskeyAddingSessionToken] that identifies this PasskeyAddingSession
     * @throws org.ouraircraft.auth.exceptions.TokenNotFoundException if [recoveryToken] is not found or expired
     */
    suspend fun createPasskeyAddingSession(
        recoveryToken: RecoveryToken
    ): Result<Pair<PasskeyAddingSessionData, PasskeyAddingSessionToken>>


    /**
     * Get data for this PasskeyAddingSession
     * @param token: The token that identifies this session
     *
     * @return the data that belongs to this session
     *
     * @throws org.ouraircraft.auth.exceptions.TokenNotFoundException when the token does not match a session
     * @throws org.ouraircraft.db.exceptions.UserNotFoundByIDException if the id that own this session does not belong to a user
     */
    suspend fun getPasskeyAddingSession(
        token: PasskeyAddingSessionToken
    ): Result<PasskeyAddingSessionData>
}