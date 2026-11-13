package org.ouraircraft.api.service

import org.ouraircraft.auth.PasskeyAddingSessionToken
import org.ouraircraft.auth.RecoveryToken
import org.ouraircraft.auth.UserSessionToken
import org.ouraircraft.db.exceptions.SessionNotFoundByIdException
import org.ouraircraft.db.repository.SessionRepository
import org.ouraircraft.model.auth.PasskeyData
import org.ouraircraft.model.dto.sessions.PasskeyAddingSessionData
import org.ouraircraft.model.dto.sessions.UserSessionData
import org.ouraircraft.model.internal.Configuration
import org.ouraircraft.model.services.users.SessionService
import org.ouraircraft.model.services.utils.runCatchingAndMapToDomainException
import kotlin.time.Clock
import kotlin.time.Instant

internal class SessionServiceImpl(
    private val sessionRepository: SessionRepository,
    private val config: Configuration.UserSessionConfig
): SessionService {
    /**
     * Creates a PasskeyAddingSession.
     *
     * @param recoveryToken A [RecoveryToken] that points to a user/recoverytoken combination.
     *  if the RecoveryToken matches a user, a new PasskeyAddingSession is created for that user.
     *
     * @return The [PasskeyAddingSessionToken] that identifies this PasskeyAddingSession,
     *  or a failed [Result] containing
     *      [org.ouraircraft.auth.exceptions.TokenNotFoundException] if [recoveryToken] is not found or expired
     */
    override suspend fun createPasskeyAddingSession(
        recoveryToken: RecoveryToken
    ): Result<Pair<PasskeyAddingSessionData, PasskeyAddingSessionToken>> =
        runCatchingAndMapToDomainException {
            sessionRepository.createPasskeyAddingSession(recoveryToken)
        }

    /**
     * Get data for this PasskeyAddingSession
     * @param token: The token that identifies this session
     *
     * @return the data that belongs to this session
     *  or a failed [Result] containing
     *      [org.ouraircraft.auth.exceptions.TokenNotFoundException] when the token does not match a session
     *      [org.ouraircraft.db.exceptions.UserNotFoundByIDException] if the id that own this session does not belong to a user
     */
    override suspend fun getPasskeyAddingSession(
        token: PasskeyAddingSessionToken
    ): Result<PasskeyAddingSessionData> = runCatchingAndMapToDomainException {
        sessionRepository.getPasskeyAddingSession(token)
    }

    /**
     * Invalidates the session on the server side. Cookies etc. must be cleaned up elsewhere.
     * Returns a Failure with a [SessionNotFoundByIdException] if [sessionData] does not point to an existing session
     */
    override suspend fun invalidatePasskeyAddingSession(sessionData: PasskeyAddingSessionData): Result<Unit> = runCatchingAndMapToDomainException{
        sessionRepository.invalidatePasskeyAddingSession(sessionData.sessionId)
    }

    /**
     * Creates a UserSession.
     *
     *  if the RecoveryToken matches a user, a new UserSession is created for that user.
     *
     * @return The [PasskeyAddingSessionData] paired to the [PasskeyAddingSessionToken] that identifies this UserSession
     */
    override suspend fun createUserSession(
        passkeyData: PasskeyData
    ): Result<Pair<UserSessionData, UserSessionToken>> =
        runCatchingAndMapToDomainException {
            val userId = passkeyData.userId
            sessionRepository.createUserSession(userId)
        }

    /**
     * Get data for this UserSession
     * @param token: The token that identifies this session
     *
     * @return the data that belongs to this session
     *
     * @throws org.ouraircraft.auth.exceptions.TokenNotFoundException when the token does not match a session
     * @throws org.ouraircraft.db.exceptions.UserNotFoundByIDException if the id that own this session does not belong to a user
     */
    override suspend fun getUserSession(token: UserSessionToken, extendIfValid: Boolean): Result<UserSessionData> = runCatchingAndMapToDomainException {
        sessionRepository.getUserSession(token).let{ currentSessionData ->
            if (extendIfValid) extendUserSession(currentSessionData, Clock.System.now() + config.sessionTTLExtensionDuration).getOrThrow()
            else currentSessionData
        }
    }

    /**
     * Extend the expiration time of a session due to the session being used.
     * This guards the domain requirement that [UserSessionData.expiresAt] <= [UserSessionData.maxExtendedExpiresAt]
     */
    override suspend fun extendUserSession(
        userSessionData: UserSessionData,
        newExpiresAt: Instant
    ): Result<UserSessionData> = runCatchingAndMapToDomainException{
        val maximizedExpiresAt = minOf(newExpiresAt, userSessionData.maxExtendedExpiresAt)

        val newUserSessionData = userSessionData.copy(expiresAt = maximizedExpiresAt)
        // if needed , we can optimize this by only extending if TTL is small enough, so not every page call creates a DB write.
        sessionRepository.extendUserSession(newUserSessionData)

        return@runCatchingAndMapToDomainException newUserSessionData
    }

    /**
     * Invalidates the session on the server side. Cookies etc. must be cleaned up elsewhere.
     * Returns a Failure with a [SessionNotFoundByIdException] if [sessionData] does not point to an existing session
     */
    override suspend fun invalidateUserSession(sessionData: UserSessionData): Result<Unit> = runCatchingAndMapToDomainException{
        sessionRepository.invalidateUserSession(sessionData.sessionId)
    }
}

