package org.ouraircraft.api.routes.sessions

import io.ktor.http.*
import io.ktor.openapi.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import org.apache.logging.log4j.LogManager
import org.ouraircraft.api.cookies.NoSessionCookieFoundException
import org.ouraircraft.api.routes.callfunctions.getUserSession
import org.ouraircraft.auth.exceptions.AuthException
import org.ouraircraft.model.dto.convertors.toDto
import org.ouraircraft.model.dto.MeResult
import org.ouraircraft.model.services.users.SessionService
import org.ouraircraft.model.services.users.UserManagementService

private val logger = LogManager.getLogger("Route.me")

/**
 * Gets the user data of the user who is in a recovery session.
 * Can be used to check if a session is logged in.
 */
fun Route.me(sessionService: SessionService, userManagementService: UserManagementService){
    get("/api/users/me"){
        val sessionData = call.getUserSession(sessionService, sendResponseOnFail = false)
            .onFailure { e->
                return@get when(e) {
                    is NoSessionCookieFoundException -> call.respond(HttpStatusCode.NotFound)
                    is AuthException -> call.respond(HttpStatusCode.NotFound)
                    else -> call.respond(HttpStatusCode.InternalServerError, "Something unexpected went wrong (me error 1): ${e::class.simpleName}").also{
                        logger.error("unexpected error 1: {}", e.stackTraceToString())
                    }
                }
            }
            .getOrThrow()

        val userData = userManagementService.getUserById(sessionData.userId)
            .onFailure { e ->
                return@get call.respond(HttpStatusCode.InternalServerError, "Something unexpected went wrong (me error 2): ${e::class.simpleName}").also{
                    logger.error("unexpected error 2: {}", e.stackTraceToString())
                }
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
            HttpStatusCode.NotFound{
                description = "No session was found. (No user is logged in)"
            }
            HttpStatusCode.InternalServerError {
                description = "Something unexpected went wrong"
                ContentType.Text.Plain {
                    schema = jsonSchema<String>()
                }
            }
        }
    }
}