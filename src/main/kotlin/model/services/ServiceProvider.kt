package org.ouraircraft.model.services

import org.ouraircraft.api.service.SessionServiceImpl
import org.ouraircraft.auth.yubico.YubicoPasskeyCeremoniesImpl
import org.ouraircraft.db.Database
import org.ouraircraft.db.repository.*
import org.ouraircraft.db.service.DataRetrievalServiceImpl
import org.ouraircraft.db.service.PasskeyServiceImpl
import org.ouraircraft.db.service.ProposalServiceImpl
import org.ouraircraft.db.service.UserManagementServiceImpl
import org.ouraircraft.model.internal.Configuration
import org.ouraircraft.model.services.data.DataRetrievalService
import org.ouraircraft.model.services.data.ProposalService
import org.ouraircraft.model.services.factories.LinkFactory
import org.ouraircraft.model.services.users.CryptoService
import org.ouraircraft.model.services.users.PasskeyCeremonies
import org.ouraircraft.model.services.users.SessionService
import org.ouraircraft.model.services.users.UserManagementService
import org.ouraircraft.model.services.utils.createRelyingParty

class ServiceProvider(
    cryptoService: CryptoService,
    database: Database,
    configuration: Configuration
){
    private val userRepository = UserRepository(database, cryptoService, configuration.userSessionConfig)
    private val sessionRepository = SessionRepository(database, cryptoService, configuration.userSessionConfig)
    private val authRepository = AuthRepository(database)
    private val passkeyService = PasskeyServiceImpl(authRepository, userRepository)

    private val manufacturerRepository = ManufacturerRepository(database, configuration.dataRetrievalConfig)
    private val aircraftTypeRepository = AircraftTypeRepository(database, configuration.dataRetrievalConfig)
    private val aircraftRepository = AircraftRepository(database, configuration.dataRetrievalConfig)

    private val linkFactory = LinkFactory(configuration.hostName!!)

    val userManagementService: UserManagementService = UserManagementServiceImpl(userRepository, linkFactory)
    val sessionService: SessionService = SessionServiceImpl(sessionRepository, configuration.userSessionConfig)
    val passkeyCeremonies: PasskeyCeremonies = YubicoPasskeyCeremoniesImpl(createRelyingParty(configuration, passkeyService), userManagementService, passkeyService)

    val dataRetrievalService: DataRetrievalService = DataRetrievalServiceImpl(manufacturerRepository, aircraftTypeRepository, aircraftRepository)
    val proposalService: ProposalService = ProposalServiceImpl(manufacturerRepository, aircraftTypeRepository, aircraftRepository)
}