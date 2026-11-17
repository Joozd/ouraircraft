package org.ouraircraft.api.routes.data.live

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import org.ouraircraft.api.routes.callfunctions.getPositiveLongParameter
import org.ouraircraft.api.routes.callfunctions.getUser
import org.ouraircraft.auth.exceptions.AuthException
import org.ouraircraft.model.dto.convertors.toDto
import org.ouraircraft.model.dto.data.AircraftDto
import org.ouraircraft.model.dto.data.PagedResultDto
import org.ouraircraft.model.services.data.DataRetrievalService
import org.ouraircraft.model.services.users.UserManagementService

private const val OFFSET_PARAMETER_NAME = "offset"
private const val LIMIT_PARAMETER_NAME = "limit"
private const val QUERY_PARAMETER_NAME = "q"
private const val DEFAULT_LIMIT_SIZE = 20L

/**
 * Gets a page of aircraft matching a registration prefix.
 *
 * Queries shorter than the minimum search length return an empty page.
 */
internal fun Route.searchAircraft(
    userManagementService: UserManagementService,
    dataRetrievalService: DataRetrievalService
) {
    get("/api/aircraft/query") {
        val user = call.getUser(userManagementService, true)
            .getOrNull() ?: return@get

        val offset = call.getPositiveLongParameter(OFFSET_PARAMETER_NAME, 0L)
            ?: return@get call.respond(HttpStatusCode.BadRequest)
        val limit = call.getPositiveLongParameter(LIMIT_PARAMETER_NAME, DEFAULT_LIMIT_SIZE)
            ?.takeIf { it <= Int.MAX_VALUE }
            ?.toInt()
            ?: return@get call.respond(HttpStatusCode.BadRequest)

        val query = call.queryParameters[QUERY_PARAMETER_NAME]?.trim()
            ?: return@get call.respond(HttpStatusCode.BadRequest)

        dataRetrievalService.getAircraftByRegistrationPrefix(user, query, limit, offset)
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
        summary = "Search aircraft by registration prefix"
        description = """
            Gets a page of aircraft whose registrations start with the supplied query.
            Requires a valid User session and the DISPLAY_AIRCRAFT authorization.

            Leading and trailing whitespace is removed from the query.
            If the query is too short after normalization, an empty page is returned.

            Omitted pagination parameters use their default values.
        """.trimIndent()

        parameters {
            query(QUERY_PARAMETER_NAME) {
                description = "The aircraft registration prefix to search for. Queries shorter than the minimum search length after normalization return an empty page."
                required = true
                schema = jsonSchema<String>()
            }
            query(OFFSET_PARAMETER_NAME) {
                description = "The number of matching aircraft to skip. Must be non-negative. Defaults to 0."
                required = false
                schema = jsonSchema<Long>()
            }
            query(LIMIT_PARAMETER_NAME) {
                description = "The requested maximum number of aircraft to return. Must be positive and fit in an Int. Defaults to $DEFAULT_LIMIT_SIZE. The service may enforce a lower maximum."
                required = false
                schema = jsonSchema<Int>()
            }
        }

        responses {
            HttpStatusCode.OK {
                description = "A page of matching aircraft with pagination metadata. Results may be empty, including when the normalized query is too short."
                schema = jsonSchema<PagedResultDto<AircraftDto>>()
            }
            HttpStatusCode.BadRequest {
                description = "The q parameter is missing, or a pagination parameter is invalid or outside its permitted range. No response body."
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