package org.ouraircraft.services

import java.net.URI
import java.net.URLDecoder
import kotlin.text.Charsets.UTF_8
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.ouraircraft.auth.EmailVerificationToken
import org.ouraircraft.auth.PasskeyAddingSessionToken
import org.ouraircraft.auth.RecoveryToken
import org.ouraircraft.db.service.UserManagementServiceImpl
import org.ouraircraft.auth.exceptions.TokenMismatchException
import org.ouraircraft.auth.exceptions.TokenNotFoundException
import org.ouraircraft.crypt.TestingCryptoService
import org.ouraircraft.db.tables.AccountRecoveryTokenTable
import org.ouraircraft.db.tables.PasskeyAddingSessionTable
import org.ouraircraft.db.tables.PendingRegistrationTable
import org.ouraircraft.db.TestingDatabase
import org.ouraircraft.db.UserRole
import org.ouraircraft.db.tables.UserTable
import org.ouraircraft.db.exceptions.DuplicateEntryException
import org.ouraircraft.db.exceptions.EmailAlreadyInUseException
import org.ouraircraft.db.exceptions.UserAlreadyExistsException
import org.ouraircraft.model.dto.EmailVerificationData
import org.ouraircraft.model.dto.NewUserRegistrationData
import org.ouraircraft.model.dto.ScreenNameAvailable
import org.ouraircraft.db.repository.SessionRepository
import org.ouraircraft.db.repository.UserRepository
import org.ouraircraft.model.services.factories.LinkFactory
import org.ouraircraft.api.service.SessionServiceImpl
import org.ouraircraft.db.exceptions.PendingUserNotFoundException
import org.ouraircraft.model.internal.Configuration
import org.ouraircraft.model.services.users.SessionService
import org.ouraircraft.model.services.users.UserManagementService
import kotlin.io.encoding.Base64
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.uuid.Uuid

/** Exercises service contracts against isolated persistence, without calling repository operations. */
internal class UserManagementServiceImplTest {
    private lateinit var fixture: TestingDatabase
    private lateinit var userManagementService: UserManagementService
    private lateinit var sessionService: SessionService
    private val crypto = TestingCryptoService()
    private val config = Configuration()


    @BeforeTest
    fun setUp() {
        fixture = TestingDatabase()
        sessionService = SessionServiceImpl(
            SessionRepository(fixture.database, crypto, config = config.userSessionConfig),
            config.userSessionConfig
        )
        userManagementService = UserManagementServiceImpl(
            UserRepository(fixture.database, crypto, config.userSessionConfig),
            LinkFactory("accounts.example.org"),
            sessionService
        )

    }

    @AfterTest
    fun tearDown() {
        if (::fixture.isInitialized) fixture.close()
    }

    @Test
    fun `registration then verification then passkey session creates each resource in order`() = runTest {
        val activation = register("Pilot", "alice@example.org")
        assertCounts(pending = 1, users = 0, recovery = 0, sessions = 0)
        val pendingCreatedAt = transaction(fixture.database.exposed) {
            val row = PendingRegistrationTable.selectAll().single()
            assertEquals(activation.id, row[PendingRegistrationTable.id])
            assertEquals("Pilot", row[PendingRegistrationTable.screenName])
            assertContentEquals(
                crypto.hashToken(activation.token).bytes.toByteArray(),
                row[PendingRegistrationTable.verificationTokenHash],
            )
            row[PendingRegistrationTable.createdAt]
        }

        val recovery = promote(activation)
        assertCounts(pending = 0, users = 1, recovery = 1, sessions = 0)
        transaction(fixture.database.exposed) {
            val user = UserTable.selectAll().single()
            assertEquals(activation.id, user[UserTable.id])
            assertEquals("Pilot", user[UserTable.screenName])
            assertContentEquals("alice@example.org".encodeToByteArray(), user[UserTable.emailEncrypted])
            assertEquals(UserRole.USER, user[UserTable.role])
            assertFalse(user[UserTable.disabled])
            assertEquals(pendingCreatedAt, user[UserTable.createdAt])
            val token = AccountRecoveryTokenTable.selectAll().single()
            assertEquals(activation.id, token[AccountRecoveryTokenTable.userId])
            assertContentEquals(crypto.hashToken(recovery).bytes.toByteArray(), token[AccountRecoveryTokenTable.tokenHash])
            assertTrue(token[AccountRecoveryTokenTable.expiresAt] > Clock.System.now())
        }

        val result = sessionService.createPasskeyAddingSession(recovery).getOrThrow()
        assertEquals(activation.id, result.first.userId)
        assertSessionOwner(result.second, activation.id)
        assertCounts(pending = 0, users = 1, recovery = 0, sessions = 1)
    }

    @Test
    fun `registration returns an activation link on the configured frontend`() = runTest {
        val uri = userManagementService.createNewPendingUserRegistration(
            NewUserRegistrationData("Pilot", "alice@example.org"),
        ).getOrThrow()
        assertEquals("https", uri.scheme)
        assertEquals("accounts.example.org", uri.host)
        assertEquals("/verify-email", uri.path)
        val activation = activationFrom(uri)
        // The credentials returned by the link must work through the next service step.
        promote(activation)
        assertCounts(pending = 0, users = 1, recovery = 1, sessions = 0)
    }

    @Test
    fun `availability checks do not reserve a name`() = runTest {
        repeat(2) {
            assertEquals(ScreenNameAvailable("Pilot", true), userManagementService.isScreenNameAvailable("Pilot").getOrThrow())
        }
        assertCounts(pending = 0, users = 0, recovery = 0, sessions = 0)
        register("Pilot", "alice@example.org")
        assertCounts(pending = 1, users = 0, recovery = 0, sessions = 0)
    }

    @Test
    fun `pending and verified users both make a name unavailable regardless of case`() = runTest {
        val activation = register("Pilot", "alice@example.org")
        assertEquals(ScreenNameAvailable("pILOT", false), userManagementService.isScreenNameAvailable("pILOT").getOrThrow())
        promote(activation)
        assertEquals(ScreenNameAvailable("PILOT", false), userManagementService.isScreenNameAvailable("PILOT").getOrThrow())
    }

    @Test
    fun `registration checks availability again after a prior positive check`() = runTest {
        assertTrue(userManagementService.isScreenNameAvailable("Pilot").getOrThrow().isAvailable)
        register("Pilot", "alice@example.org")
        val result = userManagementService.createNewPendingUserRegistration(NewUserRegistrationData("Pilot", "other@example.org"))
        assertIs<UserAlreadyExistsException>(result.exceptionOrNull())
        assertCounts(pending = 1, users = 0, recovery = 0, sessions = 0)
    }

    @Test
    fun `duplicate pending name returns a failure without another registration`() = runTest {
        register("Pilot", "alice@example.org")
        val result = userManagementService.createNewPendingUserRegistration(NewUserRegistrationData("pILOT", "other@example.org"))
        assertIs<UserAlreadyExistsException>(result.exceptionOrNull())
        assertCounts(pending = 1, users = 0, recovery = 0, sessions = 0)
    }

    @Test
    fun `duplicate verified name returns a failure without a pending registration`() = runTest {
        promote(register("Pilot", "alice@example.org"))
        val result = userManagementService.createNewPendingUserRegistration(NewUserRegistrationData("pILOT", "other@example.org"))
        assertIs<UserAlreadyExistsException>(result.exceptionOrNull())
        assertCounts(pending = 0, users = 1, recovery = 1, sessions = 0)
    }

    @Test
    fun `duplicate pending email returns a failure for a different name`() = runTest {
        register("Pilot", "alice@example.org")
        val result = userManagementService.createNewPendingUserRegistration(NewUserRegistrationData("Other", " ALICE@EXAMPLE.ORG "))
        assertIs<EmailAlreadyInUseException>(result.exceptionOrNull())
        assertCounts(pending = 1, users = 0, recovery = 0, sessions = 0)
    }

    @Test
    fun `duplicate verified email returns a failure for a different name`() = runTest {
        promote(register("Pilot", "alice@example.org"))
        val result = userManagementService.createNewPendingUserRegistration(NewUserRegistrationData("Other", " ALICE@EXAMPLE.ORG "))
        assertIs<EmailAlreadyInUseException>(result.exceptionOrNull())
        assertCounts(pending = 0, users = 1, recovery = 1, sessions = 0)
    }

    @Test
    fun `wrong verification token leaves registration pending and allows a later correct verification`() = runTest {
        val activation = register("Pilot", "alice@example.org")
        val wrongBytes = activation.token.bytes.toByteArray().also { it[0] = (it[0].toInt() xor 1).toByte() }
        val wrongToken = EmailVerificationToken.fromBase64UrlSafe(encodeToken(wrongBytes))
        assertIs<TokenMismatchException>(userManagementService.promotePendingUserToUser(verificationData(activation.id, wrongToken)).exceptionOrNull())
        assertCounts(pending = 1, users = 0, recovery = 0, sessions = 0)
        promote(activation)
        assertCounts(pending = 0, users = 1, recovery = 1, sessions = 0)
    }

    @Test
    fun `verification token from another registration cannot promote a user`() = runTest {
        val first = register("First", "first@example.org")
        val second = register("Second", "second@example.org")
        assertIs<TokenMismatchException>(userManagementService.promotePendingUserToUser(verificationData(first.id, second.token)).exceptionOrNull())
        assertCounts(pending = 2, users = 0, recovery = 0, sessions = 0)
        promote(first)
        promote(second)
        assertCounts(pending = 0, users = 2, recovery = 2, sessions = 0)
    }

    @Test
    fun `unknown registration is reported as PendingUserNotFoundException as documented`() = runTest {
        val result = userManagementService.promotePendingUserToUser(verificationData(Uuid.random(), EmailVerificationToken.generate()))
        assertIs<PendingUserNotFoundException>(result.exceptionOrNull())
        assertCounts(pending = 0, users = 0, recovery = 0, sessions = 0)
    }

    @Test
    fun `verification cannot be replayed to issue another recovery token`() = runTest {
        val activation = register("Pilot", "alice@example.org")
        val recovery = promote(activation)
        assertTrue(userManagementService.promotePendingUserToUser(verificationData(activation.id, activation.token)).isFailure)
        assertCounts(pending = 0, users = 1, recovery = 1, sessions = 0)
        assertSessionOwner(sessionService.createPasskeyAddingSession(recovery).getOrThrow().second, activation.id)
        assertCounts(pending = 0, users = 1, recovery = 0, sessions = 1)
    }

    @Test
    fun `pending verification credentials cannot create a passkey session before promotion`() = runTest {
        val activation = register("Pilot", "alice@example.org")
        val unissuedRecovery = RecoveryToken.fromBase64UrlSafe(activation.token.toBase64UrlSafe())
        assertIs<TokenNotFoundException>(sessionService.createPasskeyAddingSession(unissuedRecovery).exceptionOrNull())
        assertCounts(pending = 1, users = 0, recovery = 0, sessions = 0)
    }

    @Test
    fun `unknown recovery token cannot create a passkey session`() = runTest {
        val issued = promote(register("Pilot", "alice@example.org"))
        val wrongBytes = issued.bytes.toByteArray().also { it[0] = (it[0].toInt() xor 1).toByte() }
        val unknown = RecoveryToken.fromBase64UrlSafe(encodeToken(wrongBytes))
        assertIs<TokenNotFoundException>(sessionService.createPasskeyAddingSession(unknown).exceptionOrNull())
        assertCounts(pending = 0, users = 1, recovery = 1, sessions = 0)
    }

    @Test
    fun `expired recovery token cannot create a passkey session`() = runTest {
        val recovery = promote(register("Pilot", "alice@example.org"))
        // Set persisted expiry rather than sleeping or relying on coroutine virtual time for JDBC.
        transaction(fixture.database.exposed) {
            AccountRecoveryTokenTable.update {
                it[AccountRecoveryTokenTable.expiresAt] = Clock.System.now() - 1.hours
            }
        }
        assertIs<TokenNotFoundException>(sessionService.createPasskeyAddingSession(recovery).exceptionOrNull())
        assertCounts(pending = 0, users = 1, recovery = 1, sessions = 0)
    }

    @Test
    fun `recovery tokens create sessions for their respective users`() = runTest {
        val first = register("First", "first@example.org")
        val second = register("Second", "second@example.org")
        val firstRecovery = promote(first)
        val secondRecovery = promote(second)
        assertNotEquals(firstRecovery.bytes, secondRecovery.bytes)
        // Reverse the session creation order to catch accidental use of the most recent user.
        val secondSession = sessionService.createPasskeyAddingSession(secondRecovery).getOrThrow().second
        val firstSession = sessionService.createPasskeyAddingSession(firstRecovery).getOrThrow().second
        assertNotEquals(firstSession.bytes, secondSession.bytes)
        assertSessionOwner(firstSession, first.id)
        assertSessionOwner(secondSession, second.id)
        assertCounts(pending = 0, users = 2, recovery = 0, sessions = 2)
    }

    @Test
    fun `name taken after registration makes promotion fail without issuing recovery credentials`() = runTest {
        val pending = register("Pilot", "alice@example.org")
        val existing = register("Other", "other@example.org")
        val recovery = promote(existing)
        // Model a concurrent name change after the availability check.
        transaction(fixture.database.exposed) {
            UserTable.update({ UserTable.id eq existing.id }) { it[UserTable.screenName] = "pILOT" }
        }
        assertIs<DuplicateEntryException>(userManagementService.promotePendingUserToUser(verificationData(pending.id, pending.token)).exceptionOrNull())
        assertCounts(pending = 1, users = 1, recovery = 1, sessions = 0)
        // Existing credentials remain usable; a failed registration cannot disturb this user.
        assertSessionOwner(sessionService.createPasskeyAddingSession(recovery).getOrThrow().second, existing.id)
        assertCounts(pending = 1, users = 1, recovery = 0, sessions = 1)
    }

    private suspend fun register(name: String, email: String): Activation = activationFrom(
        userManagementService.createNewPendingUserRegistration(NewUserRegistrationData(name, email)).getOrThrow(),
    )

    private suspend fun promote(activation: Activation): RecoveryToken =
        userManagementService.promotePendingUserToUser(verificationData(activation.id, activation.token)).getOrThrow()

    private fun activationFrom(uri: URI): Activation {
        val parameters = uri.rawQuery.split('&').associate {
            val (key, value) = it.split('=', limit = 2)
            URLDecoder.decode(key, UTF_8) to URLDecoder.decode(value, UTF_8)
        }
        return Activation(
            Uuid.parse(parameters.getValue("id")),
            EmailVerificationToken.fromBase64UrlSafe(parameters.getValue("token")),
        )
    }

    private fun verificationData(id: Uuid, token: EmailVerificationToken): EmailVerificationData =
        EmailVerificationData(id, token.toBase64UrlSafe())

    private fun encodeToken(bytes: ByteArray): String =
        Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT).encode(bytes)

    private suspend fun assertSessionOwner(token: PasskeyAddingSessionToken, userId: Uuid) {
        val session = sessionService.getPasskeyAddingSession(token).getOrThrow()
        assertEquals(userId, session.userId)
        transaction(fixture.database.exposed) {
            val row = PasskeyAddingSessionTable.selectAll()
                .where { PasskeyAddingSessionTable.tokenHash eq crypto.hashToken(token).bytes.toByteArray() }
                .single()
            assertEquals(userId, row[PasskeyAddingSessionTable.userId])
            assertEquals(session.createdAt, row[PasskeyAddingSessionTable.createdAt])
            assertEquals(session.expiresAt, row[PasskeyAddingSessionTable.expiresAt])
            assertTrue(session.expiresAt > Clock.System.now())
        }
    }

    private fun assertCounts(pending: Long, users: Long, recovery: Long, sessions: Long) {
        transaction(fixture.database.exposed) {
            assertEquals(pending, PendingRegistrationTable.selectAll().count(), "Pending registrations")
            assertEquals(users, UserTable.selectAll().count(), "Users")
            assertEquals(recovery, AccountRecoveryTokenTable.selectAll().count(), "Recovery tokens")
            assertEquals(sessions, PasskeyAddingSessionTable.selectAll().count(), "Passkey sessions")
        }
    }

    private data class Activation(val id: Uuid, val token: EmailVerificationToken)
}
