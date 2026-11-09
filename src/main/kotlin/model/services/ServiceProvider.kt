package org.ouraircraft.services

import org.ouraircraft.services.impl.UserManagementServiceImpl
import org.ouraircraft.db.Database
import org.ouraircraft.repository.SessionRepository
import org.ouraircraft.repository.UserRepository
import org.ouraircraft.services.factories.LinkFactory
import org.ouraircraft.services.impl.SessionServiceImpl

class ServiceProvider(
    cryptoService: CryptoService,
    database: Database,
    linkFactory: LinkFactory
){
    private val userRepository = UserRepository(database, cryptoService)
    private val sessionRepository = SessionRepository(database, cryptoService)
    val userManagementService: UserManagementService = UserManagementServiceImpl(userRepository, linkFactory)
    val sessionService: SessionService = SessionServiceImpl(sessionRepository)
}