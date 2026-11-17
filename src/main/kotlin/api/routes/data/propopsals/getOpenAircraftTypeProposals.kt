package org.ouraircraft.api.routes.data.propopsals

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.ouraircraft.api.routes.callfunctions.getPositiveLongParameter
import org.ouraircraft.api.routes.callfunctions.getUser
import org.ouraircraft.auth.exceptions.AuthException
import org.ouraircraft.model.dto.convertors.toDto
import org.ouraircraft.model.dto.convertors.toUncheckedDto
import org.ouraircraft.model.dto.data.PagedResultDto
import org.ouraircraft.model.dto.data.UncheckedAircraftTypeProposalDto
import org.ouraircraft.model.services.data.ProposalService
import org.ouraircraft.model.services.users.UserManagementService


private const val OFFSET_PARAMETER_NAME = "offset"
private const val LIMIT_PARAMETER_NAME = "limit"
private const val DEFAULT_LIMIT_SIZE = 20L

/**
 * Gets a page of aircraft type proposals awaiting review.
 */
fun Route.getOpenAircraftTypeProposals(
    userManagementService: UserManagementService,
    proposalService: ProposalService
) {
    get("/api/proposals/aircraftTypes/open") {
        val user = call.getUser(userManagementService, true)
            .getOrNull() ?: return@get

        val offset = call.getPositiveLongParameter(OFFSET_PARAMETER_NAME, 0L)
            ?: return@get call.respond(HttpStatusCode.BadRequest)
        val limit = call.getPositiveLongParameter(LIMIT_PARAMETER_NAME, DEFAULT_LIMIT_SIZE)
            ?.takeIf { it <= Int.MAX_VALUE }
            ?.toInt()
            ?: return@get call.respond(HttpStatusCode.BadRequest)

        proposalService.getOpenAircraftTypeProposals(user, limit, offset)
            .onSuccess { pagedResult ->
                val dto = pagedResult.toDto { it.toUncheckedDto() }
                call.respond(dto)
            }.onFailure { e ->
                if (e is AuthException) {
                    call.respond(HttpStatusCode.Forbidden, "${e::class.simpleName} - ${e.message}")
                } else {
                    throw e // server error is fine for now
                }
            }
    }.describe {
        summary = "Get open aircraft type proposals"
        description = """
            Gets a page of aircraft type proposals awaiting review.
            Requires a valid User session and the DISPLAY_OPEN_PROPOSALS or DISPLAY_OWN_OPEN_PROPOSALS authorization.
            Depending on which proposal is present, only own proposals are shown, or all open proposals (highest authorization counts)

            Omitted pagination parameters use their default values.
        """.trimIndent()

        parameters {
            query(OFFSET_PARAMETER_NAME) {
                description = "The number of proposals to skip. Must be non-negative. Defaults to 0."
                required = false
                schema = jsonSchema<Long>()
            }
            query(LIMIT_PARAMETER_NAME) {
                description = "The requested maximum number of proposals to return. Must be positive and fit in an Int. Defaults to $DEFAULT_LIMIT_SIZE. The service may enforce a lower maximum."
                required = false
                schema = jsonSchema<Int>()
            }
        }

        responses {
            HttpStatusCode.OK {
                description = "A page of open aircraft type proposals with pagination metadata. Results may be empty."
                schema = jsonSchema<PagedResultDto<UncheckedAircraftTypeProposalDto>>()
            }
            HttpStatusCode.BadRequest {
                description = "A pagination parameter is invalid or outside its permitted range. No response body."
            }
            HttpStatusCode.Forbidden {
                description = """
                    The session is missing, invalid or expired, or the user lacks the
                    DISPLAY_PROPOSED_AIRCRAFT_TYPES authorization.

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