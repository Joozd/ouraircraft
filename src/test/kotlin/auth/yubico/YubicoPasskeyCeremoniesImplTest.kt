package org.ouraircraft.auth.yubico

import com.yubico.webauthn.RelyingParty
import com.yubico.webauthn.data.RelyingPartyIdentity
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.ouraircraft.crypt.TestingCryptoService
import org.ouraircraft.db.TestingDatabase
import org.ouraircraft.db.UserRole
import org.ouraircraft.db.tables.UserTable
import org.ouraircraft.db.service.PasskeyServiceImpl
import org.ouraircraft.db.service.UserManagementServiceImpl
import org.ouraircraft.model.services.users.PasskeyService
import org.ouraircraft.model.services.factories.LinkFactory
import org.ouraircraft.db.repository.AuthRepository
import org.ouraircraft.db.repository.UserRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.uuid.Uuid


class YubicoPasskeyCeremoniesImplTest {
    private val crypto = TestingCryptoService()
    private val linkFactory = LinkFactory()

    private lateinit var fixture: TestingDatabase

    private lateinit var authrepository: AuthRepository
    private lateinit var userRepository: UserRepository

    private lateinit var passkeyService: PasskeyService

    private val rpIdentity = RelyingPartyIdentity.builder()
        .id("example.com")  // Set this to a parent domain that covers all subdomains
        // where users' credentials should be valid
        .name("Example Application")
        .build()

    private lateinit var rp: RelyingParty


    private lateinit var authService: YubicoPasskeyCeremoniesImpl
    private lateinit var userManSvc: UserManagementServiceImpl


    fun insertUser(
        id: Uuid,
        screenName: String,
        role: UserRole = UserRole.USER
    ){
        transaction(fixture.database.exposed) {
            UserTable.insert { row ->
                row[UserTable.id] = id
                row[UserTable.screenName] = screenName
                row[UserTable.emailLookup] = ByteArray(32)
                row[UserTable.emailEncrypted] = ByteArray(32)
                row[UserTable.role] = role
                row[UserTable.disabled] = false
                row[UserTable.createdAt] = Clock.System.now()
            }
        }

    }


    @BeforeTest
    fun setUp() {
        fixture = TestingDatabase()
        authrepository = AuthRepository(fixture.database)
        userRepository = UserRepository(fixture.database, crypto)
        passkeyService = PasskeyServiceImpl(authrepository, userRepository)
        rp = RelyingParty.builder()
            .identity(rpIdentity)
            .credentialRepository(CredentialRepositoryImpl(passkeyService))
            .build()

        userManSvc = UserManagementServiceImpl(userRepository, linkFactory)

        authService = YubicoPasskeyCeremoniesImpl(rp, userManSvc, passkeyService)
    }

    @AfterTest
    fun tearDown() {
        if (::fixture.isInitialized) fixture.close()
    }

    @Test
    fun `kijk even of het iets doet`(){
        runBlocking {
            val testUserId = Uuid.generateV7()
            val testUserName = "Test McUser"
            insertUser(testUserId, testUserName)
            val testResult = authService.makePasskeyCreationOptionsJsonString(testUserId)
            println("testResult: $testResult")

            println("klaar weer")
        }
    }
}