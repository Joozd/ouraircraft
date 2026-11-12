package org.ouraircraft.model.services

import com.yubico.webauthn.RelyingParty
import com.yubico.webauthn.data.RelyingPartyIdentity
import org.ouraircraft.CliArgs
import org.ouraircraft.db.service.UserManagementServiceImpl
import org.ouraircraft.db.Database
import org.ouraircraft.db.repository.SessionRepository
import org.ouraircraft.db.repository.UserRepository
import org.ouraircraft.model.services.factories.LinkFactory
import org.ouraircraft.api.service.SessionServiceImpl
import org.ouraircraft.auth.yubico.CredentialRepositoryImpl
import org.ouraircraft.auth.yubico.YubicoPasskeyCeremoniesImpl
import org.ouraircraft.db.service.PasskeyServiceImpl
import org.ouraircraft.db.repository.AuthRepository
import org.ouraircraft.model.services.users.CryptoService
import org.ouraircraft.model.services.users.PasskeyCeremonies
import org.ouraircraft.model.services.users.SessionService
import org.ouraircraft.model.services.users.UserManagementService

class ServiceProvider(
    cryptoService: CryptoService,
    database: Database,
    private val args: CliArgs // TODO replace this by a config object
){
    private val userRepository = UserRepository(database, cryptoService)
    private val sessionRepository = SessionRepository(database, cryptoService)
    private val authRepository = AuthRepository(database)
    private val passkeyService = PasskeyServiceImpl(authRepository, userRepository)

    private val linkFactory = LinkFactory(args.hostName!!)

    val userManagementService: UserManagementService = UserManagementServiceImpl(userRepository, linkFactory)
    val sessionService: SessionService = SessionServiceImpl(sessionRepository)
    val passkeyCeremonies: PasskeyCeremonies = YubicoPasskeyCeremoniesImpl(createRelyingParty(), userManagementService, passkeyService)


    private fun createRelyingParty(): RelyingParty{
        val rpIdentity = RelyingPartyIdentity.builder()
            .id(args.hostName!!)  // Set this to a parent domain that covers all subdomains
            // where users' credentials should be valid
            .name("OurAircraft") // TODO also from config
            .build()

        return RelyingParty.builder()
            .identity(rpIdentity)
            .credentialRepository(CredentialRepositoryImpl(passkeyService))
            .origins(if(args.hostName == "localhost") setOf("http://localhost") else setOf("https://${args.hostName}"))
            .build()
    }
}