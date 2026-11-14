package org.ouraircraft.services

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.ouraircraft.auth.UserAuthorization
import org.ouraircraft.auth.exceptions.UserNotAuthorizedForThisException
import org.ouraircraft.crypt.EncryptedAesGcmText
import org.ouraircraft.db.TestingDatabase
import org.ouraircraft.db.exceptions.AlreadyCheckedException
import org.ouraircraft.db.repository.AircraftRepository
import org.ouraircraft.db.repository.AircraftTypeRepository
import org.ouraircraft.db.repository.ManufacturerRepository
import org.ouraircraft.db.service.ProposalServiceImpl
import org.ouraircraft.model.auth.User
import org.ouraircraft.model.data.AircraftProposal
import org.ouraircraft.model.data.AircraftTypeProposal
import org.ouraircraft.model.data.CheckResult
import org.ouraircraft.model.data.ManufacturerProposal
import org.ouraircraft.model.data.valueclasses.AircraftRegistration
import org.ouraircraft.model.internal.Configuration
import org.ouraircraft.model.services.data.ProposalService
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

class ProposalServiceImplTest {
    private lateinit var fixture: TestingDatabase
    private lateinit var proposalService: ProposalService
    private val config = Configuration().dataRetrievalConfig

    private lateinit var manufacturerRepository: ManufacturerRepository
    private lateinit var aircraftTypeRepository: AircraftTypeRepository
    private lateinit var aircraftRepository: AircraftRepository

    private val userAllAuthorizations = user(authorizations = allAuthorizationsExcept())


    @BeforeTest
    fun setUp() {
        fixture = TestingDatabase()
        manufacturerRepository = ManufacturerRepository(fixture.database, config)
        aircraftTypeRepository = AircraftTypeRepository(fixture.database, config)
        aircraftRepository = AircraftRepository(fixture.database, config)
        proposalService = ProposalServiceImpl(manufacturerRepository, aircraftTypeRepository, aircraftRepository)
    }

    @AfterTest
    fun tearDown() {
        if (::fixture.isInitialized) fixture.close()
    }

    @Test
    fun `a Manufacturer proposal can be added and then found`(){
        val user = user(authorizations = setOf(UserAuthorization.PROPOSE_MANUFACTURERS))
        val proposal = manufacturerProposal(user)
        runBlocking{
            assertTrue(proposalService.proposeManufacturer(user, proposal).isSuccess)
            val foundProposals = proposalService.getOpenManufacturerProposals(userAllAuthorizations).getOrThrow()
            assertEquals(1, foundProposals.results.size)
            val foundProposal = foundProposals.results.first()
            assertEquals(foundProposal.authorId, proposal.authorId) // very rough round-trip check.
        }
    }

    @Test
    fun `a Manufacturer proposal can not be added without correct authorization`(){
        val user = user(authorizations = allAuthorizationsExcept(UserAuthorization.PROPOSE_MANUFACTURERS))
        val proposal = manufacturerProposal(user)
        println("user had authorizations: ${user.authorizations}")
        runBlocking{
            val result = (proposalService.proposeManufacturer(user, proposal))
            assertIs<UserNotAuthorizedForThisException>(result.exceptionOrNull())
        }
    }

    @Test
    fun `an AircraftType proposal can be added and then found`() {
        val user = user(authorizations = setOf(UserAuthorization.PROPOSE_AIRCRAFT_TYPES))
        val proposal = aircraftTypeProposal(user)
        runBlocking {
            assertTrue(proposalService.proposeAircraftType(user, proposal).isSuccess)
            val foundProposals = proposalService.getOpenAircraftTypeProposals(userAllAuthorizations).getOrThrow()
            assertEquals(1, foundProposals.results.size)
            val foundProposal = foundProposals.results.first()
            assertEquals(foundProposal.authorId, proposal.authorId) // very rough round-trip check.
        }
    }

    @Test
    fun `an AircraftType proposal can not be added without correct authorization`() {
        val user = user(
            authorizations = allAuthorizationsExcept(UserAuthorization.PROPOSE_AIRCRAFT_TYPES)
        )
        val proposal = aircraftTypeProposal(user)
        runBlocking {
            val result = proposalService.proposeAircraftType(user, proposal)
            assertIs<UserNotAuthorizedForThisException>(result.exceptionOrNull())
        }
    }

    @Test
    fun `an Aircraft proposal can be added and then found`() {
        val user = user(authorizations = setOf(UserAuthorization.PROPOSE_AIRCRAFT))
        val proposal = aircraftProposal(user)
        runBlocking {
            assertTrue(proposalService.proposeAircraft(user, proposal).isSuccess)
            val foundProposals = proposalService.getOpenAircraftProposals(userAllAuthorizations).getOrThrow()
            assertEquals(1, foundProposals.results.size)
            val foundProposal = foundProposals.results.first()
            assertEquals(foundProposal.authorId, proposal.authorId) // very rough round-trip check.
        }
    }

    @Test
    fun `an Aircraft proposal can not be added without correct authorization`() {
        val user = user(
            authorizations = allAuthorizationsExcept(UserAuthorization.PROPOSE_AIRCRAFT)
        )
        val proposal = aircraftProposal(user)
        runBlocking {
            val result = proposalService.proposeAircraft(user, proposal)
            assertIs<UserNotAuthorizedForThisException>(result.exceptionOrNull())
        }
    }

    @Test
    fun `a proposal cannot be found unless user is authorized for that type`(){
        runBlocking {
            val userNoManufacturer = user(authorizations = allAuthorizationsExcept(UserAuthorization.DISPLAY_PROPOSED_MANUFACTURERS))
            val userNoAircraft = user(authorizations = allAuthorizationsExcept(UserAuthorization.DISPLAY_PROPOSED_AIRCRAFT))
            val userNoAircraftType = user(authorizations = allAuthorizationsExcept(UserAuthorization.DISPLAY_PROPOSED_AIRCRAFT_TYPES))

            assertIs<UserNotAuthorizedForThisException>(proposalService.getOpenManufacturerProposals(userNoManufacturer).exceptionOrNull())
            assertIs<UserNotAuthorizedForThisException>(proposalService.getOpenAircraftTypeProposals(userNoAircraftType).exceptionOrNull())
            assertIs<UserNotAuthorizedForThisException>(proposalService.getOpenAircraftProposals(userNoAircraft).exceptionOrNull())
        }
    }

    //re-checking should give a Result with an AlreadyCheckedException
    @Test
    fun `A Manufacturer proposal can be accepted, but only once, and with correct authorization`(){
        val user = user(authorizations = setOf(UserAuthorization.DECIDE_ON_PROPOSED_MANUFACTURERS))
        val unauthorizedUser = user(authorizations = allAuthorizationsExcept(UserAuthorization.DECIDE_ON_PROPOSED_MANUFACTURERS))
        val proposal = manufacturerProposal(user)
        runBlocking {
            //insert a proposal, and get it with the inserted ID:
            proposalService.proposeManufacturer(userAllAuthorizations, proposal)
            val foundProposalId = proposalService.getOpenManufacturerProposals(userAllAuthorizations).getOrThrow().results.first().id
            val unauthorizedResult = proposalService.handleManufacturerProposalCheck(unauthorizedUser, foundProposalId, CheckResult.REJECTED, null)
            assertIs<UserNotAuthorizedForThisException>(unauthorizedResult.exceptionOrNull())
            val checkResult = proposalService.handleManufacturerProposalCheck(user, foundProposalId, CheckResult.REJECTED, null)
            assertTrue(checkResult.isSuccess)
            val recheckResult = proposalService.handleManufacturerProposalCheck(user, foundProposalId, CheckResult.REJECTED, null)
            assertIs<AlreadyCheckedException>(recheckResult.exceptionOrNull())
        }
    }

    //re-checking should give a Result with an AlreadyCheckedException
    @Test
    fun `An Aircraft Type proposal can be accepted, but only once, and with correct authorization`(){
        val user = user(authorizations = setOf(UserAuthorization.DECIDE_ON_PROPOSED_AIRCRAFT_TYPES))
        val unauthorizedUser = user(authorizations = allAuthorizationsExcept(UserAuthorization.DECIDE_ON_PROPOSED_AIRCRAFT_TYPES))
        val proposal = aircraftTypeProposal(user)
        runBlocking {
            //insert a proposal, and get it with the inserted ID:
            proposalService.proposeAircraftType(userAllAuthorizations, proposal)
            val foundProposalId = proposalService.getOpenAircraftTypeProposals(userAllAuthorizations).getOrThrow().results.first().id
            val unauthorizedResult = proposalService.handleAircraftTypeProposalCheck(unauthorizedUser, foundProposalId, CheckResult.REJECTED, null)
            assertIs<UserNotAuthorizedForThisException>(unauthorizedResult.exceptionOrNull())
            val checkResult = proposalService.handleAircraftTypeProposalCheck(user, foundProposalId, CheckResult.REJECTED, null)
            assertTrue(checkResult.isSuccess)
            val recheckResult = proposalService.handleAircraftTypeProposalCheck(user, foundProposalId, CheckResult.REJECTED, null)
            assertIs<AlreadyCheckedException>(recheckResult.exceptionOrNull())
        }
    }

    //re-checking should give a Result with an AlreadyCheckedException
    @Test
    fun `An Aircraft proposal can be accepted, but only once, and with correct authorization`(){
        val user = user(authorizations = setOf(UserAuthorization.DECIDE_ON_PROPOSED_AIRCRAFT))
        val unauthorizedUser = user(authorizations = allAuthorizationsExcept(UserAuthorization.DECIDE_ON_PROPOSED_AIRCRAFT))
        val proposal = aircraftProposal(user)
        runBlocking {
            //insert a proposal, and get it with the inserted ID:
            proposalService.proposeAircraft(userAllAuthorizations, proposal)
            val foundProposalId = proposalService.getOpenAircraftProposals(userAllAuthorizations).getOrThrow().results.first().id
            val unauthorizedResult = proposalService.handleAircraftProposalCheck(unauthorizedUser, foundProposalId, CheckResult.REJECTED, null)
            assertIs<UserNotAuthorizedForThisException>(unauthorizedResult.exceptionOrNull())
            val checkResult = proposalService.handleAircraftProposalCheck(user, foundProposalId, CheckResult.REJECTED, null)
            assertTrue(checkResult.isSuccess)
            val recheckResult = proposalService.handleAircraftProposalCheck(user, foundProposalId, CheckResult.REJECTED, null)
            assertIs<AlreadyCheckedException>(recheckResult.exceptionOrNull())
        }
    }

    /**
     * Creates a manufacturer proposal with defaults for tests.
     *
     */
    private fun manufacturerProposal(
        user: User,
        targetId: Long? = null,
        delete: Boolean = false,
        proposalComment: String? = null,
        name: String = "Boeing",
        longName: String? = null,
    ): ManufacturerProposal = ManufacturerProposal(
        user = user,
        targetId = targetId,
        delete = delete,
        proposalComment = proposalComment,
        name = name,
        longName = longName,
    )

    /** Creates an aircraft proposal with defaults for tests. */
    private fun aircraftProposal(
        user: User,
        targetId: Long? = null,
        delete: Boolean = false,
        proposalComment: String? = null,
        aircraftTypeId: Long = 1,
        registration: AircraftRegistration = AircraftRegistration("PHBHA"),
    ): AircraftProposal = AircraftProposal(
        user = user,
        targetId = targetId,
        delete = delete,
        proposalComment = proposalComment,
        aircraftTypeId = aircraftTypeId,
        registration = registration,
    )

    /** Creates an aircraft type proposal with defaults for tests. */
    private fun aircraftTypeProposal(
        user: User,
        targetId: Long? = null,
        delete: Boolean = false,
        proposalComment: String? = null,
        manufacturerId: Long = 1,
        family: String = "777",
        variant: String = "300ER",
        icaoDesignator: String = "B77W",
        engineCount: Int = 2,
        engineType: String = "JET",
        wakeCategory: String = "H"
    ): AircraftTypeProposal = AircraftTypeProposal(
        user = user,
        targetId = targetId,
        delete = delete,
        proposalComment = proposalComment,
        manufacturerId =manufacturerId,
        family= family,
        variant = variant,
        icaoDesignator = icaoDesignator,
        engineCount = engineCount,
        engineType = engineType,
        wakeCategory = wakeCategory
    )

    /**
     * Make a user with no authorizations
     */
    private fun user(
        uuid: Uuid = Uuid.generateV7(),
        screenName: String = "John Q. User",
        encryptedMail: EncryptedAesGcmText = EncryptedAesGcmText(ByteArray(16)),
        createdAt: Instant = Clock.System.now(),
        authorizations: Set<UserAuthorization> = emptySet()
    ) = User(uuid, screenName, encryptedMail, createdAt, authorizations)

    private fun allAuthorizationsExcept(vararg missingAuths: UserAuthorization): Set<UserAuthorization> =
        UserAuthorization.entries.filter { it !in missingAuths}.toSet()
}