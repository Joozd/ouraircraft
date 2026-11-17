package org.ouraircraft.api.routes.data.live

import io.ktor.http.*
import io.ktor.openapi.jsonSchema
import io.ktor.server.response.*
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import org.ouraircraft.api.routes.callfunctions.getUser
import org.ouraircraft.auth.exceptions.AuthException
import org.ouraircraft.model.dto.convertors.toDto
import org.ouraircraft.model.dto.data.ManufacturerDto
import org.ouraircraft.model.services.data.DataRetrievalService
import org.ouraircraft.model.services.users.UserManagementService

/**
 * Get a Manufacturer by ID
 */
internal fun Route.getManufacturer(
    userManagementService: UserManagementService,
    dataRetrievalService: DataRetrievalService
) {
    /**
     * Gets a single Manufacturer by ID. If not found, this will give a 404 Not Found.
     */
    get("/api/manufacturers/{itemId}") {
        val itemId = call.parameters["itemId"]?.toLongOrNull()
            ?: return@get call.respond(HttpStatusCode.BadRequest)
        val user = call.getUser(userManagementService, true)
            .getOrNull() ?: return@get

        dataRetrievalService.getManufacturerById(user, itemId)
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
        summary = "Get a manufacturer by ID"
        description = """
        Gets a single manufacturer by its ID.
        Requires a valid User session and the DISPLAY_MANUFACTURERS authorization.

        Returns 404 Not Found if the manufacturer does not exist.
    """.trimIndent()

        parameters {
            path("itemId") {
                description = "The ID of the manufacturer to retrieve."
                required = true
                schema = jsonSchema<Long>()
            }
        }

        responses {
            HttpStatusCode.OK {
                description = "The requested manufacturer."
                schema = jsonSchema<ManufacturerDto>()
            }
            HttpStatusCode.BadRequest {
                description = "The itemId is not a valid Long. No response body."
            }
            HttpStatusCode.NotFound {
                description = "No manufacturer exists with the supplied ID. No response body."
            }
            HttpStatusCode.Forbidden {
                description = """
                The session is missing, invalid or expired, or the user lacks the
                DISPLAY_MANUFACTURERS authorization.

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