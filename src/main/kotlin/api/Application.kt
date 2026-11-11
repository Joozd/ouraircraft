package org.ouraircraft.api

import io.ktor.http.*
import io.ktor.openapi.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import org.ouraircraft.api.routes.health
import org.ouraircraft.api.routes.sessions.sessionRoutes
import org.ouraircraft.api.routes.userManagement.userManagementRoutes
import org.ouraircraft.model.services.ServiceProvider

/**
 * Configures the HTTP API and its interactive OpenAPI documentation.
 */
fun Application.module(serviceProvider: ServiceProvider) {
    install(ContentNegotiation) {
        json()
    }

    routing {
        health()
        sessionRoutes(serviceProvider)
        userManagementRoutes(serviceProvider)

        swaggerUI("/swagger") {
            info = OpenApiInfo("OurAircraft API", "1.0.0")
            source = OpenApiDocSource.Routing(
                contentType = ContentType.Application.Json
            )
        }
    }
}