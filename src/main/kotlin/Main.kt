package org.ouraircraft

import org.ouraircraft.api.ApiServer
import org.ouraircraft.db.createInMemoryDB
import org.ouraircraft.model.services.CryptoService
import org.ouraircraft.model.services.ServiceProvider

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main(args: Array<String>) {
    println("args:${args.contentToString()}")
    val args = CliArgs(args)
        .also { println(it) }

//    val configFile = args.getOrNull(0)
//        ?: "/etc/ouraircraft/config"
//
//    val config = getConfigFromFile(configFile)

    val serviceProvider = buildServiceProvider(args)

    ApiServer(serviceProvider).start()
}


//TODO get fallback settings from config
private fun buildServiceProvider(args: CliArgs): ServiceProvider{
    if(args.inMemoryDb){
        val cryptoService = CryptoService.dummy()
        val database = createInMemoryDB()
        return ServiceProvider(cryptoService, database, args)
    }
    else TODO("Create production ServiceProvider")
}

