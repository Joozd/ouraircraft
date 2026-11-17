package org.ouraircraft.api.routes.data.live

import io.ktor.http.*
import io.ktor.openapi.jsonSchema
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.ouraircraft.api.routes.callfunctions.getPositiveLongParameter
import org.ouraircraft.api.routes.callfunctions.getUser
import org.ouraircraft.auth.exceptions.AuthException
import org.ouraircraft.model.dto.convertors.toDto
import org.ouraircraft.model.dto.data.ManufacturerDto
import org.ouraircraft.model.dto.data.PagedResultDto
import org.ouraircraft.model.services.data.DataRetrievalService
import org.ouraircraft.model.services.users.UserManagementService

private const val OFFSET_PARAMETER_NAME = "offset"
private const val LIMIT_PARAMETER_NAME = "limit"
private const val QUERY_PARAMETER_NAME = "q"
private const val DEFAULT_LIMIT_SIZE = 20L


internal fun Route.searchManufacturers(
    userManagementService: UserManagementService,
    dataRetrievalService: DataRetrievalService
) {
    /**
     * Gets a page of Manufacturers. Too short a query will not yield results.
     */
    get("/api/manufacturers/query") {
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

        //This will return an empty page if trimmed query is too short.
        dataRetrievalService.getManufacturersByNamePrefix(user, query, limit, offset)
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
        summary = "Search manufacturers by name prefix"
        description = """
        Gets a page of manufacturers whose names start with the supplied query.
        Requires a valid User session and the DISPLAY_MANUFACTURERS authorization.

        Leading and trailing whitespace is removed from the query.
        If the trimmed query is too short, an empty page is returned.

        Omitted pagination parameters use their default values.
    """.trimIndent()

        parameters {
            query(QUERY_PARAMETER_NAME) {
                description = "The manufacturer name prefix to search for. Queries shorter than the minimum search length return an empty page."
                required = true
                schema = jsonSchema<String>()
            }
            query(OFFSET_PARAMETER_NAME) {
                description = "The number of matching manufacturers to skip. Must be non-negative. Defaults to 0."
                required = false
                schema = jsonSchema<Long>()
            }
            query(LIMIT_PARAMETER_NAME) {
                description = "The requested maximum number of manufacturers to return. Must be positive and fit in an Int. Defaults to $DEFAULT_LIMIT_SIZE. The service may enforce a lower maximum."
                required = false
                schema = jsonSchema<Int>()
            }
        }

        responses {
            HttpStatusCode.OK {
                description = "A page of matching manufacturers with pagination metadata. Results may be empty, including when the trimmed query is too short."
                schema = jsonSchema<PagedResultDto<ManufacturerDto>>()
            }
            HttpStatusCode.BadRequest {
                description = "The q parameter is missing, or a pagination parameter is invalid or outside its permitted range. No response body."
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