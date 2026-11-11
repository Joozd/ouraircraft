package org.ouraircraft.model.services

import org.ouraircraft.auth.PasskeyAddingSessionToken
import org.ouraircraft.auth.RecoveryToken
import org.ouraircraft.auth.UserSessionToken
import org.ouraircraft.model.auth.PasskeyData
import org.ouraircraft.model.dto.sessions.PasskeyAddingSessionData
import org.ouraircraft.model.dto.sessions.UserSessionData
import kotlin.time.Instant

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


    /**
     * Invalidates the session on the server side. Cookies etc. must be cleaned up elsewhere.
     */
    suspend fun invalidatePasskeyAddingSession(sessionData: PasskeyAddingSessionData): Result<Unit>

    /**
     * Creates a UserSession.
     *
     *  if the RecoveryToken matches a user, a new UserSession is created for that user.
     *
     * @return The [PasskeyAddingSessionData] paired to the [PasskeyAddingSessionToken] that identifies this UserSession
     */
    suspend fun createUserSession(passkeyData: PasskeyData): Result<Pair<UserSessionData, UserSessionToken>>
    // maybe change this so it requires logging in?


    /**
     * Get data for this UserSession
     * @param token: The token that identifies this session
     * @param extendIfValid: If true, this will extend the validity of the current session to the
     *
     * @return the data that belongs to this session
     *
     * @throws org.ouraircraft.auth.exceptions.TokenNotFoundException when the token does not match a session
     * @throws org.ouraircraft.db.exceptions.UserNotFoundByIDException if the id that own this session does not belong to a user
     */
    suspend fun getUserSession(
        token: UserSessionToken,
        extendIfValid: Boolean = true
    ): Result<UserSessionData>

    /**
     * Extend the expiration time of a session due to the session being used.
     * @return the [UserSessionData] with the updated TTL
     */
    suspend fun extendUserSession(userSessionData: UserSessionData, newExpiresAt: Instant): Result<UserSessionData>

    /**
     * Invalidates the session on the server side. Cookies etc. must be cleaned up elsewhere.
     */
    suspend fun invalidateUserSession(sessionData: UserSessionData): Result<Unit>
}