@file:Suppress("unused", "UnusedReceiverParameter")

package org.ouraircraft.db.repository.ops

import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.ouraircraft.auth.EmailVerificationToken
import org.ouraircraft.crypt.EncryptedAesGcmText
import org.ouraircraft.crypt.HashedEmail
import org.ouraircraft.crypt.HashedToken
import org.ouraircraft.db.tables.AccountRecoveryTokenTable
import org.ouraircraft.db.tables.PendingRegistrationTable
import org.ouraircraft.db.UserRole
import org.ouraircraft.db.tables.UserTable
import org.ouraircraft.db.conversion.toAuthorizations
import org.ouraircraft.db.exceptions.EmailAlreadyInUseException
import org.ouraircraft.db.exceptions.PendingUserNotFoundException
import org.ouraircraft.db.exceptions.UserAlreadyExistsException
import org.ouraircraft.db.exceptions.UserNotFoundByEmailException
import org.ouraircraft.db.exceptions.UserNotFoundByIDException
import org.ouraircraft.db.exceptions.UserNotFoundByScreenNameException
import org.ouraircraft.model.auth.PendingUserRegistration
import org.ouraircraft.model.auth.User
import org.ouraircraft.db.repository.UserRepository
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Inserts a new user into the database.
 *
 * The caller supplies the user ID, hashed email lookup value, and encrypted
 * email payload. Cryptographic keys remain outside the database.
 * Role authorization is the responsibility of the calling service;
 * ROOT privileges may only be assigned through local configuration.
 *
 * @param id Unique identifier of the user.
 * @param screenName Display name of the user.
 * @param emailLookup Hashed email value used for lookups and uniqueness checks.
 * @param emailEncrypted Encrypted email address.
 * @param role Initial role assigned to the user.
 * @param disabled Whether the user account is initially disabled.
 * @param createdAt Timestamp representing the account's creation.
 *
 * This function must be called within an active [JdbcTransaction].
 * Database constraint violations propagate to the caller.
 */
context(tx: JdbcTransaction)
internal fun UserRepository.createUserInTransaction(
    id: Uuid,
    screenName: String,
    emailLookup: HashedEmail,
    emailEncrypted: EncryptedAesGcmText,
    role: UserRole,
    disabled: Boolean = false,
    createdAt: Instant,
) {
    UserTable.insert { row ->
        row[UserTable.id] = id
        row[UserTable.screenName] = screenName
        row[UserTable.emailLookup] = emailLookup.bytes.toByteArray()
        row[UserTable.emailEncrypted] = emailEncrypted.bytes.toByteArray()
        row[UserTable.role] = role
        row[UserTable.disabled] = disabled
        row[UserTable.createdAt] = createdAt
    }
}

/**
 * Retrieves a pending user registration by its UUID.
 *
 * Reconstructs the registration from its persisted values without decrypting
 * the email address or verification token.
 *
 * @param uuid Unique identifier of the pending registration.
 * @return The matching pending user registration.
 * @throws PendingUserNotFoundException If no registration exists for the UUID.
 */
context(tx: JdbcTransaction)
internal fun UserRepository.getPendingUserFromTableByUuid(uuid: Uuid): PendingUserRegistration =
    PendingRegistrationTable
        .selectAll()
        .where { PendingRegistrationTable.id eq uuid }
        .singleOrNull()
        ?.let { row ->
            PendingUserRegistration(
                id = row[PendingRegistrationTable.id],
                screenName = row[PendingRegistrationTable.screenName],
                emailLookup = HashedEmail(row[PendingRegistrationTable.emailLookup]),
                emailEncrypted = EncryptedAesGcmText(row[PendingRegistrationTable.emailEncrypted]),
                verificationTokenHash = HashedToken(row[PendingRegistrationTable.verificationTokenHash]),
                createdAt = row[PendingRegistrationTable.createdAt],
                expiresAt = row[PendingRegistrationTable.expiresAt]
            )
        } ?: throw PendingUserNotFoundException(uuid)

/**
 * Creates a pending user registration awaiting email verification.
 *
 * Generates a UUIDv7 identifier, hashes and encrypts the email address,
 * and stores a hash of the verification token. The original token and
 * plaintext email address are not persisted.
 *
 * The creation timestamp is generated using [Clock.System].
 * Expiration is determined by the database schema.
 *
 * @param screenName Requested display name.
 * @param email Email address to associate with the registration.
 * @param emailVerificationToken Token issued for email verification.
 * @param expiresAt Timestamp when the registration expires.
 *
 * Database constraint violations propagate to the caller.
 */
context(tx: JdbcTransaction)
internal fun UserRepository.createPendingUserRegistrationInTransaction (
    screenName: String,
    email: String,
    emailVerificationToken: EmailVerificationToken,
    expiresAt: Instant,
): Uuid {
    val id = Uuid.generateV7()
    val emailLookup = crypto.hashEmail(email)
    val emailEncrypted = crypto.encryptEmail(email)
    val verificationTokenHash = crypto.hashToken(emailVerificationToken)

    val createdAt = Clock.System.now()

    PendingRegistrationTable.insert { row ->
        row[PendingRegistrationTable.id] = id
        row[PendingRegistrationTable.screenName] = screenName
        row[PendingRegistrationTable.emailLookup] = emailLookup.bytes.toByteArray()
        row[PendingRegistrationTable.emailEncrypted] = emailEncrypted.bytes.toByteArray()
        row[PendingRegistrationTable.verificationTokenHash] = verificationTokenHash.bytes.toByteArray()
        row[PendingRegistrationTable.createdAt] = createdAt
        row[PendingRegistrationTable.expiresAt] = expiresAt
    }

    return id
}

/**
 * Creates a registered user from an existing pending registration.
 *
 * Preserves the pending registration's UUID, display name, encrypted email,
 * email lookup hash, and creation timestamp.
 *
 * Does not delete the pending registration. The caller is responsible for
 * removing it within the same transaction when completing registration.
 *
 * @param pendingUser Pending registration containing the user data.
 * @param role Role assigned to the new user. Defaults to [UserRole.USER].
 *
 * @return the [Uuid] of the created user
 *
 * Database constraint violations propagate to the caller.
 */
context(tx: JdbcTransaction)
internal fun UserRepository.createUserFromPendingRegistration(
    pendingUser: PendingUserRegistration,
    role: UserRole = UserRole.USER
): Uuid {
    createUserInTransaction(
        id = pendingUser.id,
        screenName = pendingUser.screenName,
        emailLookup = pendingUser.emailLookup,
        emailEncrypted = pendingUser.emailEncrypted,
        role = role,
        disabled = false,
        createdAt = pendingUser.createdAt
    )
    return pendingUser.id
}

/**
 * Deletes a pending user registration by its UUID.
 *
 * @param uuid Unique identifier of the registration to delete.
 * @throws PendingUserNotFoundException If no matching registration exists.
 */
context(tx: JdbcTransaction)
internal fun UserRepository.deletePendingUserRegistration(uuid: Uuid) {
    PendingRegistrationTable.deleteWhere { id eq uuid }
        .also{
            if (it == 0) {
                throw PendingUserNotFoundException(uuid)
            }
        }
}

/**
 * Finds a registered user by email address.
 *
 * Uses the configured cryptographic implementation to derive the email
 * lookup hash. The encrypted email address is returned as part of the
 * user model without being decrypted.
 *
 * @param email Email address to search for.
 * @return The matching user.
 * @throws UserNotFoundByEmailException If no user matches the email address.
 */
context(tx: JdbcTransaction)
internal fun UserRepository.findUserByEmailInTransaction(email: String): User {
    val emailLookup = crypto.hashEmail(email)

    return UserTable
        .selectAll()
        .where { UserTable.emailLookup eq emailLookup.bytes.toByteArray() }
        .singleOrNull()
        ?.let { row ->
            User(
                id = row[UserTable.id],
                screenName = row[UserTable.screenName],
                encryptedEmail = EncryptedAesGcmText(row[UserTable.emailEncrypted]),
                authorizations = row[UserTable.role].toAuthorizations(),
                createdAt = row[UserTable.createdAt]
            )
        } ?: throw UserNotFoundByEmailException(email)
}

/**
 * Finds a registered user by screen name.
 *
 * @param screenName Email address to search for.
 * @return The matching user.
 * @throws UserNotFoundByScreenNameException If no user matches the email address.
 */
context(tx: JdbcTransaction)
internal fun UserRepository.findUserByScreenNameInTransaction(screenName: String): User {
    return UserTable
        .selectAll()
        .where { UserTable.screenName eq screenName }
        .singleOrNull()
        ?.let { row ->
            User(
                id = row[UserTable.id],
                screenName = row[UserTable.screenName],
                encryptedEmail = EncryptedAesGcmText(row[UserTable.emailEncrypted]),
                authorizations = row[UserTable.role].toAuthorizations(),
                createdAt = row[UserTable.createdAt]
            )
        } ?: throw UserNotFoundByScreenNameException(screenName)
}

/**
 * Retrieves a registered user by its unique identifier.
 *
 * Reconstructs the user from persisted values, retaining the email address
 * in encrypted form.
 *
 * @param id Unique identifier of the user.
 * @return The matching user.
 * @throws UserNotFoundByIDException If no user exists with the given ID.
 */
context(tx: JdbcTransaction)
internal fun UserRepository.getUserByIdInTransaction(
    id: Uuid
): User = UserTable
    .selectAll()
    .where { UserTable.id eq id }
    .singleOrNull()
    ?.let { row ->
        User(
            id = row[UserTable.id],
            screenName = row[UserTable.screenName],
            encryptedEmail = EncryptedAesGcmText(row[UserTable.emailEncrypted]),
            authorizations = row[UserTable.role].toAuthorizations(),
            createdAt = row[UserTable.createdAt]
        )
    } ?: throw UserNotFoundByIDException(id)

/**
 * Checks whether a screen name is available in both the user and
 * pending registration tables.
 *
 * Comparison is case-insensitive through SQLite's NOCASE collation.
 *
 * @return `true` if the screen name is available.
 */
context(tx: JdbcTransaction)
internal fun UserRepository.isScreenNameAvailableInTransaction(name: String): Boolean =
    UserTable.selectAll()
        .where { UserTable.screenName eq name }
        .empty() &&
            PendingRegistrationTable.selectAll()
                .where { PendingRegistrationTable.screenName eq name }
                .empty()

/**
 * Checks whether an email address is available in both the user and
 * pending registration tables.
 *
 * The email address is compared using its cryptographic lookup hash.
 *
 * @return `true` if the email address is available.
 */
context(tx: JdbcTransaction)
internal fun UserRepository.isEmailAvailableInTransaction(email: String): Boolean {
    val emailLookup = crypto.hashEmail(email).bytes.toByteArray()

    return UserTable.selectAll()
        .where { UserTable.emailLookup eq emailLookup }
        .empty() &&
            PendingRegistrationTable.selectAll()
                .where { PendingRegistrationTable.emailLookup eq emailLookup }
                .empty()
}

/**
 * Verifies that a screen name and email address are available for registration.
 *
 * Must be called within the same SQLite write transaction that creates the
 * registration, with the write lock acquired before these checks.
 *
 * @throws UserAlreadyExistsException if the screen name is unavailable.
 * @throws EmailAlreadyInUseException if the email address is unavailable.
 */
context(tx: JdbcTransaction)
internal fun UserRepository.checkNameAndEmailAvailableInUserAndPending(
    name: String,
    email: String,
) {
    if (!isScreenNameAvailableInTransaction(name)) {
        throw UserAlreadyExistsException(name)
    }

    if (!isEmailAvailableInTransaction(email)) {
        throw EmailAlreadyInUseException(email)
    }
}

/**
 * Inserts a recovery token into the database.
 * Expired tokens are not cleaned up by this function.
 */
context(tx: JdbcTransaction)
internal fun UserRepository.insertRecoveryToken(userId: Uuid, tokenHash: HashedToken, expiresAt: Instant){
    AccountRecoveryTokenTable.insert { row ->
        row[AccountRecoveryTokenTable.userId] = userId
        row[AccountRecoveryTokenTable.tokenHash] = tokenHash.bytes.toByteArray()
        row[AccountRecoveryTokenTable.expiresAt] = expiresAt
    }
}

