package org.ouraircraft.db.repository

import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.ouraircraft.auth.PasskeyAddingSessionToken
import org.ouraircraft.auth.RecoveryToken
import org.ouraircraft.auth.UserSessionToken
import org.ouraircraft.auth.exceptions.TokenNotFoundException
import org.ouraircraft.db.Database
import org.ouraircraft.db.exceptions.SessionNotFoundByIdException
import org.ouraircraft.db.repository.ops.*
import org.ouraircraft.model.dto.sessions.PasskeyAddingSessionData
import org.ouraircraft.model.dto.sessions.UserSessionData
import org.ouraircraft.model.internal.Configuration
import org.ouraircraft.model.services.users.CryptoService
import kotlin.time.Clock
import kotlin.uuid.Uuid

class SessionRepository(
    private val database: Database,
    val crypto: CryptoService,
    private val config: Configuration.UserSessionConfig
) {
    /**
     * Create a new PasskeyAddingSession for the user identified by [recoveryToken].
     * This consumes the token, it cannot be used again.
     *
     * @return a [Pair] of [PasskeyAddingSessionData] to its [PasskeyAddingSessionToken]
     * @throws TokenNotFoundException if [recoveryToken] is not found or has expired.
     */
    fun createPasskeyAddingSession(recoveryToken: RecoveryToken): Pair<PasskeyAddingSessionData, PasskeyAddingSessionToken> {
        val sessionToken = PasskeyAddingSessionToken.generate()

        //In this transaction, PasskeyAddingSessionData is created, then inserted into table, and recoveryToken is consumed.
        val sessionData = transaction(database.exposed) {
            val userId = getUserFromRecoveryToken(recoveryToken)

            insertPasskeyAddingSession(userId, sessionToken, config.passkeySessionTTL).also{
                invalidateRecoveryToken(recoveryToken)
            }

        }

        return sessionData to sessionToken
    }

    /**
     * Get the [PasskeyAddingSessionToken] that belongs to the session identified by [token]
     */
    fun getPasskeyAddingSession(token: PasskeyAddingSessionToken): PasskeyAddingSessionData =
        transaction(database.exposed) {
            getPasskeyAddingSessionDataFromToken(token)
        }


    /**
     * Create a new PasskeyAddingSession for the user identified by [userId].
     * This consumes the token, it cannot be used again.
     *
     * @return the stored [UserSessionData]
     */
    fun createUserSession(userId: Uuid): Pair<UserSessionData, UserSessionToken> {
        val sessionToken = UserSessionToken.generate()
        val now = Clock.System.now()

        //In this transaction, PasskeyAddingSessionData is created, then inserted into table, and recoveryToken is consumed.
        val sessionData = transaction(database.exposed) {
            UserSessionData(
                sessionId = -1,
                userId = userId,
                createdAt = now,
                expiresAt = now + config.userSessionTTL,
                maxExtendedExpiresAt = now + config.maxExtendedSessionTTL
            ).also{ sessionData ->
                insertUserSession(sessionData, sessionToken)
            }
        }

        return sessionData to sessionToken
    }

    /**
     * Get the [PasskeyAddingSessionToken] that belongs to the session identified by [token]
     */
    fun getUserSession(token: UserSessionToken): UserSessionData =
        transaction(database.exposed) {
            getUserSessionDataFromToken(token)
        }

    /**
     * Extend the user session to [UserSessionData.expiresAt]
     */
    fun extendUserSession(sessionData: UserSessionData) {
        transaction(database.exposed) {
            extendUserSessionInTransaction(sessionData.sessionId, sessionData.expiresAt)
        }
    }

    /**
     * Invalidates the session with id [userSessionId]
     * @throws SessionNotFoundByIdException if no session with id [userSessionId] was found
     */
    fun invalidateUserSession(userSessionId: Long){
        transaction(database.exposed) {
            invalidateUserSessionInTransaction(userSessionId)
        }
    }

    /**
     * Invalidates the session with id [passkeyAddingSessionId]
     * @throws SessionNotFoundByIdException if no session with id [passkeyAddingSessionId] was found
     */
    fun invalidatePasskeyAddingSession(passkeyAddingSessionId: Long){
        transaction(database.exposed) {
            invalidatePasskeyAddingSessionInTransaction(passkeyAddingSessionId)
        }
    }
}