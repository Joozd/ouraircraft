package org.ouraircraft.api.routes.userManagement

import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.ouraircraft.model.dto.ScreenNameAvailable
import org.ouraircraft.model.services.users.UserManagementService

fun Route.checkScreenNameAvailable(userManagementService: UserManagementService) {
    get("/api/usernames/{username}/availability") {
        val username = call.parameters["username"]
            ?: throw BadRequestException("Missing username")

        userManagementService.isScreenNameAvailable(username)
            .onSuccess { result ->
                call.respond(HttpStatusCode.OK, message = result)
            }
            .onFailure { throw it }
    }.describe {
        summary = "Check username availability"
        description = "Checks whether a username is available for registration."

        responses {
            HttpStatusCode.OK {
                description = "Username availability successfully checked."
                schema = jsonSchema<ScreenNameAvailable>()
            }
        }
    }
}