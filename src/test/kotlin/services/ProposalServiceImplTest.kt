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

private const val CONFIG_LIMIT_MATCHES_PER_PAGE = 5

class ProposalServiceImplTest {
    private lateinit var fixture: TestingDatabase
    private lateinit var proposalService: ProposalService
    private val config = Configuration().dataRetrievalConfig.copy(maxMatchesPerPage = CONFIG_LIMIT_MATCHES_PER_PAGE)

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
            val foundProposals = proposalService.getOpenManufacturerProposals(userAllAuthorizations, limit = 10).getOrThrow()
            assertEquals(1, foundProposals.results.size)
            val foundProposal = foundProposals.results.first()
            assertEquals(foundProposal.authorId, proposal.authorId) // very rough round-trip check.
        }
    }

    @Test
    fun `a Manufacturer proposal can not be added without correct authorization`(){
        val user = user(authorizations = allAuthorizationsExcept(UserAuthorization.PROPOSE_MANUFACTURERS))
        val proposal = manufacturerProposal(user)
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
            val foundProposals = proposalService.getOpenAircraftTypeProposals(userAllAuthorizations, limit = 10).getOrThrow()
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
            val foundProposals = proposalService.getOpenAircraftProposals(userAllAuthorizations, limit = 10).getOrThrow()
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
            val userNoDisplayProposals = user(authorizations = allAuthorizationsExcept(UserAuthorization.DISPLAY_OPEN_PROPOSALS))

            assertIs<UserNotAuthorizedForThisException>(proposalService.getOpenManufacturerProposals(userNoDisplayProposals, limit = 10).exceptionOrNull())
            assertIs<UserNotAuthorizedForThisException>(proposalService.getOpenAircraftTypeProposals(userNoDisplayProposals, limit = 10).exceptionOrNull())
            assertIs<UserNotAuthorizedForThisException>(proposalService.getOpenAircraftProposals(userNoDisplayProposals, limit = 10).exceptionOrNull())
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
            val foundProposalId = proposalService.getOpenManufacturerProposals(userAllAuthorizations, limit = 10).getOrThrow().results.first().id
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
            val foundProposalId = proposalService.getOpenAircraftTypeProposals(userAllAuthorizations, limit = 10).getOrThrow().results.first().id
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
            val foundProposalId = proposalService.getOpenAircraftProposals(userAllAuthorizations, limit = 10).getOrThrow().results.first().id
            val unauthorizedResult = proposalService.handleAircraftProposalCheck(unauthorizedUser, foundProposalId, CheckResult.REJECTED, null)
            assertIs<UserNotAuthorizedForThisException>(unauthorizedResult.exceptionOrNull())
            val checkResult = proposalService.handleAircraftProposalCheck(user, foundProposalId, CheckResult.REJECTED, null)
            assertTrue(checkResult.isSuccess)
            val recheckResult = proposalService.handleAircraftProposalCheck(user, foundProposalId, CheckResult.REJECTED, null)
            assertIs<AlreadyCheckedException>(recheckResult.exceptionOrNull())
        }
    }

    @Test
    fun `check paging is correct for Manufacturers for both own limit and Config limit`(){
        // insert 15 manufacturer proposals
        var ownLimit = 3
        val configLimit =  CONFIG_LIMIT_MATCHES_PER_PAGE
        val entriesInDb = 15
        require (ownLimit < configLimit) {"Cannot do this check if smallLimit is not less than configLimit"}
        require(entriesInDb > configLimit) {"Cannot do this check if entriesInDb is not more than configLimit"}
        runBlocking {

            repeat(entriesInDb) {
                val manufacturerProposal = manufacturerProposal(userAllAuthorizations)
                proposalService.proposeManufacturer(userAllAuthorizations, manufacturerProposal)
            }
            // check own limit is respected for all pages, including offset
            var currentOffset = 0L
            while(currentOffset<= entriesInDb){
                val page = proposalService.getOpenManufacturerProposals(userAllAuthorizations, ownLimit, currentOffset)
                    .getOrThrow()
                // check the page knows how many total results there are
                assertEquals(entriesInDb.toLong(), page.totalCount)
                //check page has the correct offset
                assertEquals(currentOffset, page.offset)
                // Check page knows what limit this has
                assertEquals(ownLimit, page.limit)
                val expectedResultsOnThisPage = minOf(ownLimit.toLong(), entriesInDb - currentOffset).toInt()
                //Check the page has the correct number of results
                assertEquals(expectedResultsOnThisPage, page.results.size)

                currentOffset += ownLimit
            }
            //check offset past last result gives empty page
            currentOffset = entriesInDb + 1L
            val page = proposalService.getOpenManufacturerProposals(userAllAuthorizations, ownLimit, currentOffset)
                .getOrThrow()
            // check the page knows how many total results there are
            assertEquals(entriesInDb.toLong(), page.totalCount)
            //check page has the correct offset
            assertEquals(currentOffset, page.offset)
            // Check page knows what limit this has
            assertEquals(ownLimit, page.limit)
            //Check the page has the correct number of results
            assertEquals(0, page.results.size)

            // check config limit is respected and communicated correctly
            currentOffset = 0
            ownLimit = configLimit + 1
            while(currentOffset<= entriesInDb){
                val page = proposalService.getOpenManufacturerProposals(userAllAuthorizations, ownLimit, currentOffset)
                    .getOrThrow()
                // check the page knows how many total results there are
                assertEquals(entriesInDb.toLong(), page.totalCount)
                //check page has the correct offset
                assertEquals(currentOffset, page.offset)
                // Check page knows what limit this has
                assertEquals(configLimit, page.limit)
                val expectedResultsOnThisPage = minOf(configLimit.toLong(), entriesInDb - currentOffset).toInt()
                //Check the page has the correct number of results
                assertEquals(expectedResultsOnThisPage, page.results.size)

                currentOffset += configLimit
            }
        }
    }

    /**
     * Verifies pagination of aircraft type proposals with a requested limit
     * and with the configured maximum limit, including offsets beyond the results.
     */
    @Test
    fun `check paging is correct for AircraftTypes for both own limit and Config limit`() {
        val ownLimit = 3
        val configLimit = CONFIG_LIMIT_MATCHES_PER_PAGE
        val entriesInDb = 15

        require(ownLimit < configLimit) {
            "Cannot do this check if ownLimit is not less than configLimit"
        }
        require(entriesInDb > configLimit) {
            "Cannot do this check if entriesInDb is not more than configLimit"
        }

        runBlocking {
            repeat(entriesInDb) {
                val proposal = aircraftTypeProposal(userAllAuthorizations)
                proposalService.proposeAircraftType(userAllAuthorizations, proposal)
                    .getOrThrow()
            }

            // Check the requested limit, including an offset at the end.
            var currentOffset = 0L
            while (currentOffset <= entriesInDb) {
                val page = proposalService.getOpenAircraftTypeProposals(
                    userAllAuthorizations, ownLimit, currentOffset
                ).getOrThrow()

                assertEquals(entriesInDb.toLong(), page.totalCount)
                assertEquals(currentOffset, page.offset)
                assertEquals(ownLimit, page.limit)

                val expectedResultsOnThisPage =
                    minOf(ownLimit.toLong(), entriesInDb - currentOffset).toInt()
                assertEquals(expectedResultsOnThisPage, page.results.size)

                currentOffset += ownLimit
            }

            // Check an offset beyond the last result.
            currentOffset = entriesInDb + 1L
            val page = proposalService.getOpenAircraftTypeProposals(
                userAllAuthorizations, ownLimit, currentOffset
            ).getOrThrow()

            assertEquals(entriesInDb.toLong(), page.totalCount)
            assertEquals(currentOffset, page.offset)
            assertEquals(ownLimit, page.limit)
            assertEquals(0, page.results.size)

            // Request more than the configured maximum.
            val requestedLimit = configLimit + 1
            currentOffset = 0L

            while (currentOffset <= entriesInDb) {
                val limitedPage = proposalService.getOpenAircraftTypeProposals(
                    userAllAuthorizations, requestedLimit, currentOffset
                ).getOrThrow()

                assertEquals(entriesInDb.toLong(), limitedPage.totalCount)
                assertEquals(currentOffset, limitedPage.offset)
                assertEquals(configLimit, limitedPage.limit)

                val expectedResultsOnThisPage =
                    minOf(configLimit.toLong(), entriesInDb - currentOffset).toInt()
                assertEquals(expectedResultsOnThisPage, limitedPage.results.size)

                currentOffset += configLimit
            }
        }
    }

    /**
     * Verifies pagination of aircraft proposals with a requested limit
     * and with the configured maximum limit, including offsets beyond the results.
     */
    @Test
    fun `check paging is correct for Aircraft for both own limit and Config limit`() {
        val ownLimit = 3
        val configLimit = CONFIG_LIMIT_MATCHES_PER_PAGE
        val entriesInDb = 15

        require(ownLimit < configLimit) {
            "Cannot do this check if ownLimit is not less than configLimit"
        }
        require(entriesInDb > configLimit) {
            "Cannot do this check if entriesInDb is not more than configLimit"
        }

        runBlocking {
            repeat(entriesInDb) {
                val proposal = aircraftProposal(userAllAuthorizations)
                proposalService.proposeAircraft(userAllAuthorizations, proposal)
                    .getOrThrow()
            }

            // Check the requested limit, including an offset at the end.
            var currentOffset = 0L
            while (currentOffset <= entriesInDb) {
                val page = proposalService.getOpenAircraftProposals(
                    userAllAuthorizations, ownLimit, currentOffset
                ).getOrThrow()

                assertEquals(entriesInDb.toLong(), page.totalCount)
                assertEquals(currentOffset, page.offset)
                assertEquals(ownLimit, page.limit)

                val expectedResultsOnThisPage =
                    minOf(ownLimit.toLong(), entriesInDb - currentOffset).toInt()
                assertEquals(expectedResultsOnThisPage, page.results.size)

                currentOffset += ownLimit
            }

            // Check an offset beyond the last result.
            currentOffset = entriesInDb + 1L
            val page = proposalService.getOpenAircraftProposals(
                userAllAuthorizations, ownLimit, currentOffset
            ).getOrThrow()

            assertEquals(entriesInDb.toLong(), page.totalCount)
            assertEquals(currentOffset, page.offset)
            assertEquals(ownLimit, page.limit)
            assertEquals(0, page.results.size)

            // Request more than the configured maximum.
            val requestedLimit = configLimit + 1
            currentOffset = 0L

            while (currentOffset <= entriesInDb) {
                val limitedPage = proposalService.getOpenAircraftProposals(
                    userAllAuthorizations, requestedLimit, currentOffset
                ).getOrThrow()

                assertEquals(entriesInDb.toLong(), limitedPage.totalCount)
                assertEquals(currentOffset, limitedPage.offset)
                assertEquals(configLimit, limitedPage.limit)

                val expectedResultsOnThisPage =
                    minOf(configLimit.toLong(), entriesInDb - currentOffset).toInt()
                assertEquals(expectedResultsOnThisPage, limitedPage.results.size)

                currentOffset += configLimit
            }
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