package org.ouraircraft.api.routes.sessions

import io.ktor.http.*
import io.ktor.openapi.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import org.ouraircraft.api.cookies.deleteUserSessionCookie
import org.ouraircraft.api.routes.callfunctions.getUserSession
import org.ouraircraft.model.services.SessionService

/**
 * Logs out the current session
 */
fun Route.logout(sessionService: SessionService){
    post("/api/users/logout"){
        val sessionData = call.getUserSession(sessionService, sendResponseOnFail = true)
            .getOrNull() ?: return@post

        call.deleteUserSessionCookie()
        sessionService.invalidateUserSession(sessionData)
        call.respond(HttpStatusCode.NoContent)
    }.describe {
        summary = "Logs out the current session"
        description = "Requires a valid User session cookie. " +
                "This will invalidate the User Session cookie and end the session on the server side."

        responses {
            HttpStatusCode.NoContent {
                description = "The session was successfully invalidated."
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