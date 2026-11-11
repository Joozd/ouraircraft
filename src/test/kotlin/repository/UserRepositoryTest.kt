package org.ouraircraft.repository

import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.ouraircraft.auth.EmailVerificationToken
import org.ouraircraft.crypt.TestingCryptoService
import org.ouraircraft.db.tables.PendingRegistrationTable
import org.ouraircraft.db.TestingDatabase
import org.ouraircraft.db.UserRole
import org.ouraircraft.db.tables.UserTable
import org.ouraircraft.db.exceptions.*
import org.ouraircraft.db.repository.UserRepository
import org.ouraircraft.model.auth.PendingUserRegistration
import org.ouraircraft.model.auth.User
import org.ouraircraft.db.repository.ops.createUserInTransaction
import org.sqlite.SQLiteException
import kotlin.io.encoding.Base64
import kotlin.test.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlin.uuid.Uuid

internal class UserRepositoryTest {
    private lateinit var fixture: TestingDatabase
    private lateinit var repository: UserRepository
    private val crypto = TestingCryptoService()
    private var tokenSequence = 0
    private val verificationTokens = mutableMapOf<Uuid, EmailVerificationToken>()

    @BeforeTest
    fun setUp() {
        fixture = TestingDatabase()
        repository = UserRepository(fixture.database, crypto)
        tokenSequence = 0
        verificationTokens.clear()
    }

    @AfterTest
    fun tearDown() {
        if (::fixture.isInitialized) fixture.close()
    }

    @Test
    fun `direct creation persists supplied fields and defaults to enabled`() {
        val email = "Alice@example.org"
        val expected = addUser(email, "Pilot Å", UserRole.SUPERUSER)
        assertUserLookups(expected, email)
        transaction(fixture.database.exposed) {
            val row = UserTable.selectAll().where { UserTable.id eq expected.id }.single()
            assertContentEquals(crypto.hashEmail(email).bytes.toByteArray(), row[UserTable.emailLookup])
            assertContentEquals(email.encodeToByteArray(), row[UserTable.emailEncrypted])
            assertEquals(UserRole.SUPERUSER, row[UserTable.role])
            assertEquals(false, row[UserTable.disabled])
        }
        assertCounts(1, 0)
    }

    @Test
    fun `direct creation stores every supplied role`() {
        for ((index, role) in UserRole.entries.withIndex()) {
            val email = "role$index@example.org"
            val expected = addUser(email, role = role)
            assertUserLookups(expected, email)
            transaction(fixture.database.exposed) {
                assertEquals(role, UserTable.selectAll().where { UserTable.id eq expected.id }.single()[UserTable.role])
            }
        }
    }

    @Test
    fun `lookup methods return disabled users`() {
        val expected = addUser("disabled@example.org", disabled = true)
        assertUserLookups(expected, "disabled@example.org")
        transaction(fixture.database.exposed) {
            assertEquals(true, UserTable.selectAll().where { UserTable.id eq expected.id }.single()[UserTable.disabled])
        }
    }

    @Test
    fun `pending creation persists registration fields and expiration`() {
        val email = "Alice@example.org"
        val token = nextToken()
        val before = Clock.System.now()
        val id = repository.createPendingUserRegistration("Pilot Å", email, token)
        val after = Clock.System.now()
        val pending = repository.getPendingUserRegistrationByUuid(id)

        assertEquals("Pilot Å", pending.screenName)
        assertEquals(crypto.hashEmail(email), pending.emailLookup)
        assertEquals(crypto.encryptEmail(email), pending.emailEncrypted)
        assertEquals(crypto.hashToken(token), pending.verificationTokenHash)
        assertTrue(pending.createdAt.toEpochMilliseconds() in before.toEpochMilliseconds()..after.toEpochMilliseconds())
        assertTrue(pending.expiresAt.toEpochMilliseconds() in (before + 24.hours).toEpochMilliseconds()..(after + 24.hours).toEpochMilliseconds())
        assertEquals('7', pending.id.toString()[14])
        assertFailsWith<UserNotFoundByIDException> { repository.getUserById(pending.id) }
        assertCounts(0, 1)
    }

    @Test
    fun `promotion moves pending registration preserving its fields`() {
        val email = "alice@example.org"
        val pending = addPending("Pilot Å", email)
        assertEquals(pending.id, repository.elevatePendingToUser(pending.id, verificationTokens.getValue(pending.id)))
        assertUserLookups(
            User(pending.id, pending.screenName, pending.emailEncrypted, pending.createdAt, emptySet()), email,
        )
        transaction(fixture.database.exposed) {
            val row = UserTable.selectAll().where { UserTable.id eq pending.id }.single()
            assertContentEquals(pending.emailLookup.bytes.toByteArray(), row[UserTable.emailLookup])
            assertEquals(false, row[UserTable.disabled])
        }
        assertFailsWith<PendingUserNotFoundException> { repository.getPendingUserRegistrationByUuid(pending.id) }
        assertCounts(1, 0)
    }

    @Test
    fun `promotion leaves other pending registrations unchanged`() {
        val first = addPending("First pilot", "first@example.org")
        val second = addPending("Second pilot", "second@example.org")
        repository.elevatePendingToUser(first.id, verificationTokens.getValue(first.id))
        assertEquals(second, repository.getPendingUserRegistrationByUuid(second.id))
        assertFailsWith<UserNotFoundByIDException> { repository.getUserById(second.id) }
        assertCounts(1, 1)
    }

    @Test
    fun `registration rejects duplicate screen name in users`() =
        assertRegistrationConflict(inUsers = true, duplicateName = true)

    @Test
    fun `registration rejects duplicate screen name in pending`() =
        assertRegistrationConflict(inUsers = false, duplicateName = true)

    @Test
    fun `registration rejects case variant of name in users`() =
        assertRegistrationConflict(inUsers = true, duplicateName = true, normalizedVariant = true)

    @Test
    fun `registration rejects case variant of name in pending`() =
        assertRegistrationConflict(inUsers = false, duplicateName = true, normalizedVariant = true)

    @Test
    fun `registration rejects duplicate email in users`() =
        assertRegistrationConflict(inUsers = true, duplicateName = false)

    @Test
    fun `registration rejects duplicate email in pending`() =
        assertRegistrationConflict(inUsers = false, duplicateName = false)

    @Test
    fun `registration rejects normalized duplicate email in users`() =
        assertRegistrationConflict(inUsers = true, duplicateName = false, normalizedVariant = true)

    @Test
    fun `registration rejects normalized duplicate email in pending`() =
        assertRegistrationConflict(inUsers = false, duplicateName = false, normalizedVariant = true)

    @Test
    fun `promotion fails with database uniqueness violation when name was taken meanwhile`() =
        assertPromotionConflict("Pilot", "other@example.org")

    @Test
    fun `promotion fails with database uniqueness violation for case variant of taken name`() =
        assertPromotionConflict("pILOT", "other@example.org")

    @Test
    fun `promotion fails with database uniqueness violation when email was taken meanwhile`() =
        assertPromotionConflict("Other pilot", "alice@example.org")

    @Test
    fun `promotion cannot be repeated after successful move`() {
        val pending = addPending("Pilot", "alice@example.org")
        repository.elevatePendingToUser(pending.id, verificationTokens.getValue(pending.id))
        val existing = repository.getUserById(pending.id)
        assertFailsWith<PendingUserNotFoundException> { repository.elevatePendingToUser(pending.id, verificationTokens.getValue(pending.id)) }
        assertEquals(existing, repository.getUserById(pending.id))
        assertCounts(1, 0)
    }

    @Test
    fun `missing pending lookup and promotion throw specific exceptions`() {
        val missingId = Uuid.random()
        assertFailsWith<PendingUserNotFoundException> { repository.getPendingUserRegistrationByUuid(missingId) }
        assertFailsWith<PendingUserNotFoundException> { repository.elevatePendingToUser(missingId, nextToken()) }
        assertCounts(0, 0)
    }

    @Test
    fun `missing user lookups throw specific exceptions in empty and populated databases`() {
        val missingId = Uuid.random()
        repeat(2) { iteration ->
            if (iteration == 1) addUser("alice@example.org")
            assertFailsWith<UserNotFoundByEmailException> { repository.findUserByEmail("missing@example.org") }
            assertFailsWith<UserNotFoundByIDException> { repository.getUserById(missingId) }
            assertFailsWith<UserNotFoundByScreenNameException> { repository.findUserByScreenName("Missing pilot") }
        }
    }

    @Test
    fun `lookups select matching user among multiple users`() {
        addUser("other@example.org")
        val expected = addUser("alice@example.org", role = UserRole.SUPERUSER)
        assertUserLookups(expected, "alice@example.org")
    }

    @Test
    fun `email and screen name lookups use normalized comparisons`() {
        val expected = addUser("alice@example.org", "Pilot")
        assertEquals(expected, repository.findUserByEmail("  ALICE@EXAMPLE.ORG  "))
        assertEquals(expected, repository.findUserByScreenName("pILOT"))
    }

    private fun assertRegistrationConflict(
        inUsers: Boolean,
        duplicateName: Boolean,
        normalizedVariant: Boolean = false,
    ) {
        val user = if (inUsers) addUser("alice@example.org", "Pilot") else null
        val pending = if (!inUsers) addPending("Pilot", "alice@example.org") else null
        // Keep the other field distinct to isolate the intended availability check.
        val name = if (duplicateName) (if (normalizedVariant) "pILOT" else "Pilot") else "Other pilot"
        val email = if (!duplicateName) {
            if (normalizedVariant) " ALICE@EXAMPLE.ORG " else "alice@example.org"
        } else "other@example.org"
        if (duplicateName) {
            val failure = assertFailsWith<UserAlreadyExistsException> {
                repository.createPendingUserRegistration(name, email, nextToken())
            }
            assertEquals(name, failure.screenName)
        } else {
            val failure = assertFailsWith<EmailAlreadyInUseException> {
                repository.createPendingUserRegistration(name, email, nextToken())
            }
            assertEquals(email, failure.email)
        }
        if (user != null) assertUserLookups(user, "alice@example.org")
        if (pending != null) assertEquals(pending, repository.getPendingUserRegistrationByUuid(pending.id))
        assertCounts(if (inUsers) 1L else 0L, if (inUsers) 0L else 1L)
    }

    private fun assertPromotionConflict(takenName: String, takenEmail: String) {
        val pending = addPending("Pilot", "alice@example.org")
        // Direct insertion models another caller taking the name/email after registration.
        val existing = addUser(takenEmail, takenName)
        val failure = assertFailsWith<ExposedSQLException> {
            repository.elevatePendingToUser(pending.id, verificationTokens.getValue(pending.id))
        }
        val sqliteFailure = assertIs<SQLiteException>(failure.cause)
        assertEquals(2067, sqliteFailure.resultCode.code, "Expected a SQLite UNIQUE constraint violation")
        assertEquals(pending, repository.getPendingUserRegistrationByUuid(pending.id))
        assertUserLookups(existing, takenEmail)
        assertFailsWith<UserNotFoundByIDException> { repository.getUserById(pending.id) }
        assertCounts(1, 1)
    }

    private fun assertUserLookups(expected: User, email: String) {
        assertEquals(expected, repository.getUserById(expected.id))
        assertEquals(expected, repository.findUserByEmail(email))
        assertEquals(expected, repository.findUserByScreenName(expected.screenName))
    }

    private fun assertCounts(users: Long, pending: Long) {
        transaction(fixture.database.exposed) {
            assertEquals(users, UserTable.selectAll().count(), "Registered user count")
            assertEquals(pending, PendingRegistrationTable.selectAll().count(), "Pending registration count")
        }
    }

    private fun nextToken(): EmailVerificationToken {
        val marker = (++tokenSequence).toByte()
        return EmailVerificationToken.fromBase64UrlSafe(
            Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(ByteArray(32) { marker }),
        )
    }

    private fun addPending(screenName: String, email: String): PendingUserRegistration {
        val token = nextToken()
        val id = repository.createPendingUserRegistration(screenName, email, token)
        verificationTokens[id] = token
        return repository.getPendingUserRegistrationByUuid(id)
    }

    private fun addUser(
        email: String,
        screenName: String = "Pilot Å — $email",
        role: UserRole = UserRole.USER,
        disabled: Boolean = false,
    ): User {
        val id = Uuid.generateV7()
        val encryptedEmail = crypto.encryptEmail(email)
        val createdAt = Instant.parse("2025-01-02T03:04:05Z")
        transaction(fixture.database.exposed) {
            if (disabled) {
                repository.createUserInTransaction(
                    id, screenName, crypto.hashEmail(email), encryptedEmail, role,
                    disabled = true, createdAt = createdAt,
                )
            } else {
                // Exercise the default disabled argument for enabled users.
                repository.createUserInTransaction(
                    id, screenName, crypto.hashEmail(email), encryptedEmail, role, createdAt = createdAt,
                )
            }
        }
        return User(id, screenName, encryptedEmail, createdAt, emptySet())
    }
}
