package org.ouraircraft.api.routes

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe

fun Route.health() {
    get("/api/health") {
        call.respondText("OK")
    }.describe {
        summary = "Health check"
        description = "Checks whether the API server is running."

        responses {
            HttpStatusCode.OK {
                description = "The server is running."
                ContentType.Text.Plain()
            }
        }
    }
}