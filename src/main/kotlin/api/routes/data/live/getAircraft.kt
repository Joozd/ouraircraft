package org.ouraircraft.api.routes.data.live

import io.ktor.http.*
import io.ktor.openapi.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import org.ouraircraft.api.routes.callfunctions.getUser
import org.ouraircraft.auth.exceptions.AuthException
import org.ouraircraft.model.dto.convertors.toDto
import org.ouraircraft.model.dto.data.AircraftDto
import org.ouraircraft.model.services.data.DataRetrievalService
import org.ouraircraft.model.services.users.UserManagementService

/**
 * Gets an aircraft by ID, or returns 404 Not Found if it does not exist.
 */
internal fun Route.getAircraft(
    userManagementService: UserManagementService,
    dataRetrievalService: DataRetrievalService
) {
    get("/api/aircraft/{itemId}") {
        val itemId = call.parameters["itemId"]?.toLongOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest)
        val user = call.getUser(userManagementService, true)
            .getOrNull() ?: return@get

        dataRetrievalService.getAircraftById(user, itemId)
            .onSuccess { result ->
                return@get result?.let {
                    call.respond(HttpStatusCode.OK, result.toDto())
                } ?: call.respond(HttpStatusCode.NotFound)
            }.onFailure { e ->
                if (e is AuthException) {
                    call.respond(HttpStatusCode.Forbidden, "${e::class.simpleName} - ${e.message}")
                } else {
                    throw e // server error is fine for now
                }
            }
    }.describe {
        summary = "Get an aircraft by ID"
        description = """
            Gets a single aircraft by its ID.
            Requires a valid User session and the DISPLAY_AIRCRAFT authorization.

            Returns 404 Not Found if the aircraft does not exist.
        """.trimIndent()

        parameters {
            path("itemId") {
                description = "The ID of the aircraft to retrieve."
                required = true
                schema = jsonSchema<Long>()
            }
        }

        responses {
            HttpStatusCode.OK {
                description = "The requested aircraft."
                schema = jsonSchema<AircraftDto>()
            }
            HttpStatusCode.BadRequest {
                description = "The itemId is not a valid Long. No response body."
            }
            HttpStatusCode.NotFound {
                description = "No aircraft exists with the supplied ID. No response body."
            }
            HttpStatusCode.Forbidden {
                description = """
                    The session is missing, invalid or expired, or the user lacks the
                    DISPLAY_AIRCRAFT authorization.

                    Login errors return "Bad login information provided. Please re-login."
                    or "No stored session found. Please log in."
                    Authorization errors return the exception name and message.
                """.trimIndent()
                ContentType.Text.Plain()
            }
            HttpStatusCode.InternalServerError {
                description = "An unhandled server error occurred."
            }
        }
    }
}