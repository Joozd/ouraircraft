package org.ouraircraft.db.repository

import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.ouraircraft.auth.EmailVerificationToken
import org.ouraircraft.auth.RecoveryToken
import org.ouraircraft.auth.exceptions.TokenMismatchException
import org.ouraircraft.model.services.CryptoService
import org.ouraircraft.db.Database
import org.ouraircraft.db.exceptions.UserNotFoundByIDException
import org.ouraircraft.model.auth.PendingUserRegistration
import org.ouraircraft.model.auth.User
import org.ouraircraft.model.policy.pendingRegistrationTimeout
import org.ouraircraft.db.repository.ops.checkNameAndEmailAvailableInUserAndPending
import org.ouraircraft.db.repository.ops.createPendingUserRegistrationInTransaction
import org.ouraircraft.db.repository.ops.createUserFromPendingRegistration
import org.ouraircraft.db.repository.ops.deletePendingUserRegistration
import org.ouraircraft.db.repository.ops.findUserByEmailInTransaction
import org.ouraircraft.db.repository.ops.findUserByScreenNameInTransaction
import org.ouraircraft.db.repository.ops.getPendingUserFromTableByUuid
import org.ouraircraft.db.repository.ops.getUserByIdInTransaction
import org.ouraircraft.db.repository.ops.insertRecoveryToken
import org.ouraircraft.db.repository.ops.isScreenNameAvailableInTransaction
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid

//TODO make this config options
private val RECOVERY_TOKEN_TTL = 30.minutes

/**
 * Provides persistence operations for users and pending user registrations
 * using the Exposed JDBC DSL.
 *
 * Each operation runs in its own database transaction.
 *
 * @param database Database connection used for persistence.
 * @param crypto Cryptographic operations used to protect and look up user data.
 */
internal class UserRepository(
    private val database: Database,
    val crypto: CryptoService
) {
    /**
     * Creates a pending user registration awaiting email verification.
     * Throws an exception if the operation fails for any reason.
     *
     * @param screenName Display name requested by the user.
     * @param email Email address associated with the registration.
     * @param emailVerificationToken Token used to verify ownership of the email address.
     * @return Unit
     */
    fun createPendingUserRegistration(
        screenName: String,
        email: String,
        emailVerificationToken: EmailVerificationToken
    ): Uuid =
        transaction(database.exposed) {
            val expiresAt = pendingRegistrationTimeout()
            checkNameAndEmailAvailableInUserAndPending(screenName, email) // will throw if duplicate exists

            createPendingUserRegistrationInTransaction(screenName, email, emailVerificationToken, expiresAt)
        }

    /**
     * Retrieves a pending user registration by its unique identifier.
     *
     * @param uuid Unique identifier of the pending registration.
     * @return The pending registration
     */
    fun getPendingUserRegistrationByUuid(
        uuid: Uuid
    ): PendingUserRegistration =
        transaction(database.exposed) {
            getPendingUserFromTableByUuid(uuid)
        }


    /**
     * Converts a pending user registration into a registered user.
     *
     * Creates the user and removes the pending registration within a single
     * transaction, ensuring that both operations succeed or neither takes effect.
     *
     * NOTE: This does not check if the pending registration is expired, or if the email verification token is correct.
     *
     * @param id Unique identifier of the pending registration.
     * @return The identifier of the registered user
     * @throws TokenMismatchException if [emailVerificationToken] does not match the stored token hash for the user
     */
    fun elevatePendingToUser(
        id: Uuid,
        emailVerificationToken: EmailVerificationToken
    ): Uuid {
        val tokenHash = crypto.hashToken(emailVerificationToken)
        return transaction(database.exposed) {
            val pendingUser: PendingUserRegistration = getPendingUserFromTableByUuid(id)
            if(pendingUser.verificationTokenHash != tokenHash) throw TokenMismatchException("Provided token $emailVerificationToken does not match the expected token for user ${pendingUser.screenName} with id $id")

            val createdID = createUserFromPendingRegistration(pendingUser)
            deletePendingUserRegistration(pendingUser.id)

            createdID
        }
    }


    /**
     * Finds a registered user by email address.
     *
     * Uses the configured cryptographic implementation to perform the email lookup.
     *
     * @param email Email address of the user to find.
     * @return The matching user
     */
    fun findUserByEmail(email: String): User =
        transaction(database.exposed) {
            findUserByEmailInTransaction(email)
        }


    /**
     * Finds a registered user by screen name.
     *
     * @param screenName Screen name of the user to find.
     * @return The matching user
     */
    fun findUserByScreenName(screenName: String): User =
        transaction(database.exposed) {
            findUserByScreenNameInTransaction(screenName)
        }


    /**
     * Retrieves a registered user by its unique identifier.
     *
     * @param id Unique identifier of the user.
     * @return The matching user
     *
     * @throws UserNotFoundByIDException If no user exists with the given ID.
     */
    fun getUserById(id: Uuid): User =
        transaction(database.exposed) {
            getUserByIdInTransaction(id)
        }


    /**
     * Check if [screenName] appears to be available to register.
     * No guarantees that it will be available later as well, just right now.
     */
    fun isScreenNameAvailable(screenName: String): Boolean =
        transaction(database.exposed) {
            isScreenNameAvailableInTransaction(screenName)
        }

    /**
     * Create a recovery token for user with id [userId], and store it in [org.ouraircraft.db.tables.AccountRecoveryTokenTable]
     * @throws UserNotFoundByIDException If no user exists with the given ID.
     */
    fun createRecoveryToken(userId: Uuid): RecoveryToken = transaction(database.exposed) {
        val user = getUserByIdInTransaction(userId) // make sure user exists
        val recoveryToken = RecoveryToken.generate()
        val recoveryTokenHash = crypto.hashToken(recoveryToken)
        val expiresAt = Clock.System.now() + RECOVERY_TOKEN_TTL
        insertRecoveryToken(user.id, recoveryTokenHash, expiresAt)

        recoveryToken
    }
}