package org.ouraircraft.repository

import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.ouraircraft.crypt.TestingCrypto
import org.ouraircraft.db.TestingDatabase
import org.ouraircraft.db.UserRole
import org.ouraircraft.db.UserTable
import org.ouraircraft.model.User
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

internal class UserRepositoryTest {
    private lateinit var fixture: TestingDatabase
    private lateinit var repository: UserRepository
    private val crypto = TestingCrypto()

    @BeforeTest
    fun setUp() {
        fixture = TestingDatabase()
        repository = UserRepository(fixture.database, crypto)
    }

    @AfterTest
    fun tearDown() {
        if (::fixture.isInitialized) fixture.close()
    }

    @Test
    fun `createUser persists new screen name email default role enabled state and current timestamp`() {
        val email = "Alice@example.org"
        val screenName = "Pilot Å"
        val before = Clock.System.now()
        val id = repository.createUser(email, screenName)
        val after = Clock.System.now()

        transaction(fixture.database.exposed) {
            val row = UserTable.selectAll().where { UserTable.id eq id }.single()
            assertEquals(1L, UserTable.selectAll().count())
            assertEquals(screenName, row[UserTable.screenName])
            assertContentEquals(crypto.hashEmail(email).bytes.toByteArray(), row[UserTable.emailLookup])
            assertContentEquals(email.encodeToByteArray(), row[UserTable.emailEncrypted])
            assertEquals(UserRole.USER, row[UserTable.role])
            assertEquals(false, row[UserTable.disabled])
            assertTrue(row[UserTable.createdAt] in before..after)
        }
        assertEquals('7', id.toString()[14], "New users should have UUIDv7 IDs")
        assertEquals(id, assertNotNull(repository.getUserById(id)).id)
        assertEquals(id, assertNotNull(repository.findUserByEmail(email)).id)
        assertEquals(screenName, assertNotNull(repository.getUserById(id)).screenName)
        assertEquals(screenName, assertNotNull(repository.findUserByEmail(email)).screenName)
    }

    @Test
    fun `createUser persists each explicitly supplied role`() {
        val roles = listOf(
            User.Role.USER to UserRole.USER,
            User.Role.SUPERUSER to UserRole.SUPERUSER,
            User.Role.ROOT to UserRole.ROOT,
        )
        for ((index, rolesForUser) in roles.withIndex()) {
            val (modelRole, databaseRole) = rolesForUser
            val id = repository.createUser("role$index@example.org", "Pilot $index", modelRole)
            transaction(fixture.database.exposed) {
                val row = UserTable.selectAll().where { UserTable.id eq id }.single()
                assertEquals(databaseRole, row[UserTable.role])
            }
            assertEquals(modelRole, assertNotNull(repository.getUserById(id)).role)
        }
    }

    @Test
    fun `createUser assigns different IDs to different users`() {
        val first = repository.createUser("first@example.org", "First pilot")
        val second = repository.createUser("second@example.org", "Second pilot")

        assertNotEquals(first, second)
        assertEquals(first, assertNotNull(repository.findUserByEmail("first@example.org")).id)
        assertEquals(second, assertNotNull(repository.findUserByEmail("second@example.org")).id)
    }

    @Test
    fun `createUser rejects duplicate email without replacing existing user`() {
        val existing = seedUser("alice@example.org")

        assertFailsWith<ExposedSQLException> {
            repository.createUser("alice@example.org", "New pilot", User.Role.ROOT)
        }

        assertEquals(existing, repository.findUserByEmail("alice@example.org"))
        transaction(fixture.database.exposed) {
            assertEquals(1L, UserTable.selectAll().count())
        }
    }

    @Test
    fun `createUser rejects duplicate screen name without replacing existing user`() {
        val email = "alice@example.org"
        val screenName = "Pilot Å"
        val existingId = repository.createUser(email, screenName)
        val existing = assertNotNull(repository.getUserById(existingId))

        // A different email isolates the screen-name uniqueness constraint.
        assertFailsWith<ExposedSQLException> {
            repository.createUser("other@example.org", screenName, User.Role.ROOT)
        }

        assertEquals(existing, repository.getUserById(existingId))
        assertEquals(existing, repository.findUserByEmail(email))
        assertNull(repository.findUserByEmail("other@example.org"))
        transaction(fixture.database.exposed) {
            assertEquals(1L, UserTable.selectAll().count())
        }
    }

    @Test
    fun `findUserByEmail returns null when no email matches`() {
        assertNull(repository.findUserByEmail("missing@example.org"))
        seedUser("alice@example.org")
        assertNull(repository.findUserByEmail("missing@example.org"))
    }

    @Test
    fun `getUserById returns null when no ID matches`() {
        val missingId = Uuid.parse("00000000-0000-7000-8000-000000000001")
        assertNull(repository.getUserById(missingId))
        seedUser("alice@example.org")
        assertNull(repository.getUserById(missingId))
    }

    @Test
    fun `findUserByEmail returns all fields of the matching user`() {
        seedUser("other@example.org")
        val expected = seedUser("alice@example.org", UserRole.SUPERUSER)

        assertEquals(expected, repository.findUserByEmail("alice@example.org"))
    }

    @Test
    fun `findUserByEmail uses the lookup value supplied by crypto`() {
        val expected = seedUser("alice@example.org")

        assertEquals(expected, repository.findUserByEmail("  ALICE@EXAMPLE.ORG  "))
    }

    @Test
    fun `getUserById returns all fields of the matching user for each role`() {
        val roles = listOf(UserRole.USER, UserRole.SUPERUSER, UserRole.ROOT)
        for ((index, role) in roles.withIndex()) {
            val expected = seedUser("role$index@example.org", role)
            assertEquals(expected, repository.getUserById(expected.id))
        }
    }

    @Test
    fun `lookup methods return a disabled user`() {
        val expected = seedUser("disabled@example.org", disabled = true)

        assertEquals(expected, repository.findUserByEmail("disabled@example.org"))
        assertEquals(expected, repository.getUserById(expected.id))
    }

    private fun seedUser(
        email: String,
        role: UserRole = UserRole.USER,
        disabled: Boolean = false,
    ): User {
        val id = Uuid.random()
        val screenName = "Pilot Å — $email"
        val encryptedEmail = crypto.encryptEmail(email)
        val createdAt = Instant.parse("2025-01-02T03:04:05Z")
        transaction(fixture.database.exposed) {
            UserTable.insert { row ->
                row[UserTable.id] = id
                row[UserTable.screenName] = screenName
                row[UserTable.emailLookup] = crypto.hashEmail(email).bytes.toByteArray()
                row[UserTable.emailEncrypted] = encryptedEmail.bytes.toByteArray()
                row[UserTable.role] = role
                row[UserTable.disabled] = disabled
                row[UserTable.createdAt] = createdAt
            }
        }
        val modelRole = when (role) {
            UserRole.USER -> User.Role.USER
            UserRole.SUPERUSER -> User.Role.SUPERUSER
            UserRole.ROOT -> User.Role.ROOT
        }
        return User(id, screenName, encryptedEmail, modelRole, createdAt)
    }
}
