package org.ouraircraft.api.routes.data.live

import io.ktor.http.*
import io.ktor.openapi.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import org.ouraircraft.api.routes.callfunctions.getPositiveLongParameter
import org.ouraircraft.api.routes.callfunctions.getUser
import org.ouraircraft.auth.exceptions.AuthException
import org.ouraircraft.model.dto.convertors.toDto
import org.ouraircraft.model.dto.data.AircraftTypeDto
import org.ouraircraft.model.dto.data.PagedResultDto
import org.ouraircraft.model.services.data.DataRetrievalService
import org.ouraircraft.model.services.users.UserManagementService

private const val OFFSET_PARAMETER_NAME = "offset"
private const val LIMIT_PARAMETER_NAME = "limit"
private const val DESIGNATOR_PARAMETER_NAME = "designator_prefix"
private const val FAMILY_PARAMETER_NAME = "family_prefix"
private const val DEFAULT_LIMIT_SIZE = 20L

/**
 * Gets a page of aircraft types matching a name prefix.
 *
 * Queries shorter than the minimum search length return an empty page.
 */
internal fun Route.searchAircraftTypes(
    userManagementService: UserManagementService,
    dataRetrievalService: DataRetrievalService
) {
    get("/api/aircraftTypes/query") {
        val user = call.getUser(userManagementService, true)
            .getOrNull() ?: return@get

        val offset = call.getPositiveLongParameter(OFFSET_PARAMETER_NAME, 0L)
            ?: return@get call.respond(HttpStatusCode.BadRequest)
        val limit = call.getPositiveLongParameter(LIMIT_PARAMETER_NAME, DEFAULT_LIMIT_SIZE)
            ?.takeIf { it <= Int.MAX_VALUE }
            ?.toInt()
            ?: return@get call.respond(HttpStatusCode.BadRequest)

        val designatorQuery = call.queryParameters[DESIGNATOR_PARAMETER_NAME]?.trim()
        val familyQuery = call.queryParameters[FAMILY_PARAMETER_NAME]?.trim()

        if(designatorQuery == null && familyQuery == null) return@get call.respond(HttpStatusCode.BadRequest)

        dataRetrievalService.getAircraftTypesByDesignatorOrRegistration(designatorQuery, familyQuery, user, limit, offset)
            .onSuccess { pagedResult ->
                val dto = pagedResult.toDto { it.toDto() }
                call.respond(dto)
            }.onFailure { e ->
                if (e is AuthException) {
                    call.respond(HttpStatusCode.Forbidden, "${e::class.simpleName} - ${e.message}")
                } else {
                    throw e // server error is fine for now
                }
            }
    }.describe {
        summary = "Search aircraft types by name prefix"
        description = """
            Gets a page of aircraft types whose names start with the supplied query.
            Requires a valid User session and the DISPLAY_AIRCRAFT_TYPES authorization.

            This needs at least 1 of '$DESIGNATOR_PARAMETER_NAME' and '$FAMILY_PARAMETER_NAME' parameters to be present.
            
            Leading and trailing whitespace is removed from the query.
            If the trimmed query is too short, an empty page is returned.
            
            Omitted pagination parameters use their default values.
        """.trimIndent()

        parameters {
            query(DESIGNATOR_PARAMETER_NAME) {
                description = "The aircraft type designator prefix to search for. Queries shorter than the minimum search length return an empty page."
                required = true
                schema = jsonSchema<String>()
            }
            query(FAMILY_PARAMETER_NAME) {
                description = "The aircraft family prefix to search for. Queries shorter than the minimum search length return an empty page."
                required = true
                schema = jsonSchema<String>()
            }
            query(OFFSET_PARAMETER_NAME) {
                description = "The number of matching aircraft types to skip. Must be non-negative. Defaults to 0."
                required = false
                schema = jsonSchema<Long>()
            }
            query(LIMIT_PARAMETER_NAME) {
                description = "The requested maximum number of aircraft types to return. Must be positive and fit in an Int. Defaults to $DEFAULT_LIMIT_SIZE. The service may enforce a lower maximum."
                required = false
                schema = jsonSchema<Int>()
            }
        }

        responses {
            HttpStatusCode.OK {
                description = "A page of matching aircraft types with pagination metadata. Results may be empty, including when the trimmed query is too short."
                schema = jsonSchema<PagedResultDto<AircraftTypeDto>>()
            }
            HttpStatusCode.BadRequest {
                description = "Both $DESIGNATOR_PARAMETER_NAME and $FAMILY_PARAMETER_NAME parameters is missing, or a pagination parameter is invalid or outside its permitted range. No response body."
            }
            HttpStatusCode.Forbidden {
                description = """
                    The session is missing, invalid or expired, or the user lacks the
                    DISPLAY_AIRCRAFT_TYPES authorization.

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