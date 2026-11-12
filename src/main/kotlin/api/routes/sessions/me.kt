package org.ouraircraft.api.routes.sessions

import io.ktor.http.*
import io.ktor.openapi.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import org.ouraircraft.api.routes.callfunctions.getUserSession
import org.ouraircraft.db.repository.convertors.toDto
import org.ouraircraft.model.dto.MeResult
import org.ouraircraft.model.services.users.SessionService
import org.ouraircraft.model.services.users.UserManagementService

/**
 * Gets the user data of the user who is in a recovery session.
 * Can be used to check if a session is logged in.
 */
fun Route.me(sessionService: SessionService, userManagementService: UserManagementService){
    get("/api/users/me"){
        val sessionData = call.getUserSession(sessionService, sendResponseOnFail = true)
            .getOrThrow()

        val userData = userManagementService.getUserById(sessionData.userId)
            .onFailure { e ->
                throw (e) //TODO
                // return@get
            }.getOrThrow()
            .toDto()

        call.respond(HttpStatusCode.OK, MeResult(userData, sessionData))
    }.describe {
        summary = "Get the user associated with a User session"
        description = "Returns user data for the current User session. " +
                "Requires a valid User session cookie; this indicates a regular login session."

        responses {
            HttpStatusCode.OK {
                description = "The user and session data associated with the session."
                schema = jsonSchema<MeResult>()
            }
            HttpStatusCode.Forbidden {
                description = "No User session cookie was provided, or the cookie held invalid data"
                ContentType.Text.Plain {
                    schema = jsonSchema<String>()
                }
            }
        }
    }
}