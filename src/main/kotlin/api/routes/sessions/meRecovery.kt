package org.ouraircraft.api.routes.sessions

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import org.ouraircraft.api.cookies.readPasskeyAddingCookie
import org.ouraircraft.model.dto.DtoUser
import org.ouraircraft.db.repository.convertors.toDto
import org.ouraircraft.model.services.SessionService
import org.ouraircraft.model.services.UserManagementService

/**
 * Gets the user data of the user who is in a recovery session.
 * Can be used to check if a session is logged in.
 */
fun Route.meRecovery(sessionService: SessionService, userManagementService: UserManagementService){
    get("/api/users/me-recovery"){
        val token = call.readPasskeyAddingCookie()
        if(token == null){
            call.respond(
                HttpStatusCode.Forbidden,
                "No login information provided"
            )
            return@get
        }
        val sessionData = sessionService.getPasskeyAddingSession(token)
            .onFailure { e ->
                call.respondToBadLogin(e)
                return@get
            }.getOrThrow()

        val userData = userManagementService.getUserById(sessionData.userId)
            .onFailure { e ->
                //TODO
                throw (e)
                // return@get
            }.getOrThrow()
            .toDto()

        call.respond(HttpStatusCode.OK, userData)
    }.describe {
        summary = "Get the user associated with a passkey-adding session"
        description = "Returns user data for the current passkey-adding session. " +
                "Requires a valid passkey-adding session cookie; this does not indicate " +
                "a regular login session."

        responses {
            HttpStatusCode.OK {
                description = "The user associated with the session."
                schema = jsonSchema<DtoUser>()
            }
            HttpStatusCode.Forbidden {
                description = "No passkey-adding session cookie was provided, or the cookie held invalid data"
                ContentType.Text.Plain {
                    schema = jsonSchema<String>()
                }
            }
        }
    }
}