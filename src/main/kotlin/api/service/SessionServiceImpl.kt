package org.ouraircraft.services.impl

import org.ouraircraft.auth.PasskeyAddingSessionToken
import org.ouraircraft.auth.RecoveryToken
import org.ouraircraft.db.exceptions.runCatchingAndMapToDomainException
import org.ouraircraft.model.dto.sessions.PasskeyAddingSessionData
import org.ouraircraft.repository.SessionRepository
import org.ouraircraft.repository.UserRepository
import org.ouraircraft.repository.getPasskeyAddingSessionDataFromToken
import org.ouraircraft.services.SessionService

internal class SessionServiceImpl(
    private val sessionRepository: SessionRepository
): SessionService {
    /**
     * Creates a PasskeyAddingSession.
     *
     * @param recoveryToken A [RecoveryToken] that points to a user/recoverytoken combination.
     *  if the RecoveryToken matches a user, a new PasskeyAddingSession is created for that user.
     *
     * @return The [org.ouraircraft.auth.PasskeyAddingSessionToken] that identifies this PasskeyAddingSession,
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
}

