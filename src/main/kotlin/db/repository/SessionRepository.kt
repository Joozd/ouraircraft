package org.ouraircraft.db.repository

import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.ouraircraft.auth.PasskeyAddingSessionToken
import org.ouraircraft.auth.RecoveryToken
import org.ouraircraft.auth.UserSessionToken
import org.ouraircraft.auth.exceptions.TokenNotFoundException
import org.ouraircraft.db.Database
import org.ouraircraft.db.exceptions.SessionNotFoundByIdException
import org.ouraircraft.model.dto.sessions.PasskeyAddingSessionData
import org.ouraircraft.model.dto.sessions.UserSessionData
import org.ouraircraft.model.services.users.CryptoService
import org.ouraircraft.db.repository.ops.extendUserSessionInTransaction
import org.ouraircraft.db.repository.ops.getPasskeyAddingSessionDataFromToken
import org.ouraircraft.db.repository.ops.getUserFromRecoveryToken
import org.ouraircraft.db.repository.ops.getUserSessionDataFromToken
import org.ouraircraft.db.repository.ops.insertPasskeyAddingSession
import org.ouraircraft.db.repository.ops.insertUserSession
import org.ouraircraft.db.repository.ops.invalidatePasskeyAddingSessionInTransaction
import org.ouraircraft.db.repository.ops.invalidateRecoveryToken
import org.ouraircraft.db.repository.ops.invalidateUserSessionInTransaction
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid

//TODO make these properties injected
private val PASSKEY_SESSION_TTL = 30.minutes
private val USER_SESSION_TTL = 30.days
private val MAX_EXTENDED_SESSION_TTL = 365.days * 1000 // 1000 years means no practical max extension for now

class SessionRepository(
    private val database: Database,
    val crypto: CryptoService
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

            insertPasskeyAddingSession(userId, sessionToken, PASSKEY_SESSION_TTL).also{
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
                expiresAt = now + USER_SESSION_TTL,
                maxExtendedExpiresAt = now + MAX_EXTENDED_SESSION_TTL
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