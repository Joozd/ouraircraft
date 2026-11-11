package org.ouraircraft.api

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.ouraircraft.model.services.ServiceProvider

class ApiServer(private val serviceProvider: ServiceProvider) {
    fun start(host: String = "127.0.0.1", port: Int = 8080) {
        embeddedServer(Netty, host = host, port = port) {
            module(serviceProvider)
        }.start(wait = true)
    }
}