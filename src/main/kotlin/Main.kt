package org.ouraircraft

import org.apache.logging.log4j.LogManager
import org.ouraircraft.api.ApiServer
import org.ouraircraft.db.createInMemoryDB
import org.ouraircraft.model.internal.CliArgs
import org.ouraircraft.model.internal.Configuration
import org.ouraircraft.model.services.users.CryptoService
import org.ouraircraft.model.services.ServiceProvider

private val logger = LogManager.getLogger("org.ouraircraft.MainKT")

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main(args: Array<String>) {
    logger.debug("args:${args.contentToString()}")
    val args = CliArgs(args)
        .also { logger.debug(it) }

//    val configFile = args.getOrNull(0)
//        ?: "/etc/ouraircraft/config"
//
//    val config = getConfigFromFile(configFile)

    val serviceProvider = buildServiceProvider(args.toConfiguration())

    ApiServer(serviceProvider).start()
}

private fun buildServiceProvider(configuration: Configuration): ServiceProvider{
    if(configuration.inMemoryDb){
        val cryptoService = CryptoService.dummy()
        val database = createInMemoryDB()
        return ServiceProvider(cryptoService, database, configuration)
    }
    else TODO("Create production ServiceProvider")
}

