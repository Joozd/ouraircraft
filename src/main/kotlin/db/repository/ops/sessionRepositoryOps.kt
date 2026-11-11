@file:Suppress("UNUSED_PARAMETER") // context receiver params are there to force this happening inside a transaction

package org.ouraircraft.db.repository.ops

import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greater
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.ouraircraft.auth.PasskeyAddingSessionToken
import org.ouraircraft.auth.RecoveryToken
import org.ouraircraft.auth.Token
import org.ouraircraft.auth.UserSessionToken
import org.ouraircraft.auth.exceptions.TokenNotFoundException
import org.ouraircraft.crypt.HashedToken
import org.ouraircraft.db.tables.AccountRecoveryTokenTable
import org.ouraircraft.db.tables.PasskeyAddingSessionTable
import org.ouraircraft.db.tables.UserSessionTable
import org.ouraircraft.db.exceptions.SessionNotFoundByIdException
import org.ouraircraft.model.dto.sessions.PasskeyAddingSessionData
import org.ouraircraft.model.dto.sessions.UserSessionData
import org.ouraircraft.db.repository.SessionRepository
import org.ouraircraft.db.repository.convertors.toPasskeyAddingSessionData
import org.ouraircraft.db.repository.convertors.toUserSessionData
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant
import kotlin.uuid.Uuid


private val now get() = Clock.System.now()

/**
 * invalidate a recovery token.
 * Expired tokens are OK and will still be deleted.
 * @throws TokenNotFoundException if [recoveryToken] does not exist.
 */
context(tx: JdbcTransaction)
internal fun SessionRepository.invalidateRecoveryToken(recoveryToken: RecoveryToken){
    val hash = hashToken(recoveryToken)
    val deletedRows = AccountRecoveryTokenTable.deleteWhere {
        AccountRecoveryTokenTable.tokenHash eq hash.bytes.toByteArray()
    }
    if(deletedRows == 0) throw TokenNotFoundException("Cannot invalidate token: Token is invalid")
}

/**
 * creates a PasskeyAddingSession
 * @param userId the User UUID for this session
 * @param token The [PasskeyAddingSessionToken] that locks this session
 * @param ttl The duration this session is valid
 * @param now A timestamp "now", which is stored for reference, and to determine when TTL expires.
 *
 * This does not check if [PasskeyAddingSessionData.userId] is an existing user; sessions for non-existent users do nothing.
 */
context(tx: JdbcTransaction)
internal fun SessionRepository.insertPasskeyAddingSession(
    userId: Uuid,
    token: PasskeyAddingSessionToken,
    ttl: Duration,
    now: Instant = Clock.System.now() // can be injected for testing
): PasskeyAddingSessionData{
    val hashedToken = hashToken(token)
    val expiresAt = now + ttl
    val sessionId = PasskeyAddingSessionTable.insertAndGetId { row ->
        row[PasskeyAddingSessionTable.userId] = userId
        row[PasskeyAddingSessionTable.tokenHash] = hashedToken.bytes.toByteArray()
        row[PasskeyAddingSessionTable.createdAt] = now
        row[PasskeyAddingSessionTable.expiresAt] = expiresAt
    }.value

    return PasskeyAddingSessionData(sessionId, userId, now, expiresAt)
}

/**
 * creates a USerSession
 * @param userSessionData The [PasskeyAddingSessionData] that describes this Session
 * @param token The [PasskeyAddingSessionToken] that locks this session
 *
 * This does not check if [PasskeyAddingSessionData.userId] is an existing user; sessions for non-existent users do nothing.
 * This ignores [UserSessionData.sessionId], it always generates a new primary key.
 */
context(tx: JdbcTransaction)
internal fun SessionRepository.insertUserSession(
    userSessionData: UserSessionData,
    token: UserSessionToken,
){
    val hashedToken = hashToken(token)
    UserSessionTable.insert { row ->
        row[UserSessionTable.userId] = userSessionData.userId
        row[UserSessionTable.tokenHash] = hashedToken.bytes.toByteArray()
        row[UserSessionTable.createdAt] = userSessionData.createdAt
        row[UserSessionTable.expiresAt] = userSessionData.expiresAt
        row[UserSessionTable.maxExtendedExpiresAt] = userSessionData.maxExtendedExpiresAt
    }
}

/**
 * Get the userId that comes with this RecoveryToken.
 * Any actions that are allowed from this token must be done in the same transaction,
 * and the token must be invalidated at the end of the transaction.
 * @receiver The JDBC Transaction that this is a part of.
 * @throws TokenNotFoundException if [recoveryToken] is not found or has expired.
 */
context(tx: JdbcTransaction)
internal fun SessionRepository.getUserFromRecoveryToken(
    recoveryToken: RecoveryToken
): Uuid {
    val hash = hashToken(recoveryToken)
    val result = AccountRecoveryTokenTable.selectAll()
        .where {
            (AccountRecoveryTokenTable.tokenHash eq hash.bytes.toByteArray()) and (AccountRecoveryTokenTable.expiresAt greater now)
        }.singleOrNull() ?: throw TokenNotFoundException("Cannot retrieve token info: Token is invalid or expired.")
    return result[AccountRecoveryTokenTable.userId]
}

/**
 * Get the [getPasskeyAddingSessionDataFromToken] for the Session identified by [token]
 * @throws TokenNotFoundException if [token] is not found or has expired.
 */
context(tx: JdbcTransaction)
internal fun SessionRepository.getPasskeyAddingSessionDataFromToken(
    token: PasskeyAddingSessionToken
): PasskeyAddingSessionData {
    val hashedToken = hashToken(token).bytes.toByteArray()
    val result = PasskeyAddingSessionTable.selectAll()
        .where { PasskeyAddingSessionTable.tokenHash eq hashedToken }
        .singleOrNull() ?: throw TokenNotFoundException("Cannot retrieve Session Data: Token is invalid.")

    val sessionData = result.toPasskeyAddingSessionData()

    //remove expired session from DB
    if(sessionData.expiresAt < now){
        PasskeyAddingSessionTable.deleteWhere {
            PasskeyAddingSessionTable.tokenHash eq hashedToken
        }
        throw TokenNotFoundException("Cannot retrieve Session Data: Token is expired.")
    }

    return sessionData
}

/**
 * Get the [getPasskeyAddingSessionDataFromToken] for the Session identified by [token]
 * @throws TokenNotFoundException if [token] is not found or has expired.
 */
context(tx: JdbcTransaction)
internal fun SessionRepository.getUserSessionDataFromToken(
    token: UserSessionToken
): UserSessionData {
    val hashedToken = hashToken(token).bytes.toByteArray()
    val result = UserSessionTable.selectAll()
        .where { UserSessionTable.tokenHash eq hashedToken }
        .singleOrNull() ?: throw TokenNotFoundException("Cannot retrieve Session Data: Token is invalid.")

    val sessionData = result.toUserSessionData()

    //remove expired session from DB
    if(sessionData.expiresAt < now){
        UserSessionTable.deleteWhere {
            UserSessionTable.tokenHash eq hashedToken
        }
        throw TokenNotFoundException("Cannot retrieve Session Data: Token is expired.")
    }

    return sessionData
}


/**
 * Extends the expiration of the UserSession with id [sessionId] to [newExpiresAt]
 * @throws SessionNotFoundByIdException if no session with id [sessionId] was found
 */
context(tx: JdbcTransaction)
internal fun extendUserSessionInTransaction(sessionId: Long, newExpiresAt: Instant){
    UserSessionTable.update({ UserSessionTable.id eq sessionId}) {
        it[expiresAt] = newExpiresAt
    }.also{ if (it == 0) throw SessionNotFoundByIdException(sessionId) }
}

/**
 * Invalidates the session with id [sessionId]
 * @throws SessionNotFoundByIdException if no session with id [sessionId] was found
 */
context(tx: JdbcTransaction)
internal fun invalidateUserSessionInTransaction(sessionId: Long){
    UserSessionTable.deleteWhere { UserSessionTable.id eq sessionId }
        .also{ if (it == 0) throw SessionNotFoundByIdException(sessionId) }
}


/**
 * Invalidates the session with id [sessionId]
 * @throws SessionNotFoundByIdException if no session with id [sessionId] was found
 */
context(tx: JdbcTransaction)
internal fun invalidatePasskeyAddingSessionInTransaction(sessionId: Long){
    PasskeyAddingSessionTable.deleteWhere { PasskeyAddingSessionTable.id eq sessionId }
        .also{ if (it == 0) throw SessionNotFoundByIdException(sessionId) }
}






/**
 * Centralized logic for hashing a [Token]
 */
private fun SessionRepository.hashToken(token: Token): HashedToken =
    crypto.hashToken(token)