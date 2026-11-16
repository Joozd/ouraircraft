package org.ouraircraft.api.routes.data.propopsals

import io.ktor.http.*
import io.ktor.openapi.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import org.ouraircraft.api.routes.callfunctions.getPositiveLongParameter
import org.ouraircraft.api.routes.callfunctions.getUser
import org.ouraircraft.auth.exceptions.AuthException
import org.ouraircraft.model.dto.convertors.toDto
import org.ouraircraft.model.dto.convertors.toUncheckedDto
import org.ouraircraft.model.dto.data.PagedResultDto
import org.ouraircraft.model.dto.data.UncheckedManufacturerProposalDto
import org.ouraircraft.model.services.data.ProposalService
import org.ouraircraft.model.services.users.UserManagementService


private const val OFFSET_PARAMETER_NAME = "offset"
private const val LIMIT_PARAMETER_NAME = "limit"
private const val DEFAULT_LIMIT_SIZE = 20L

/**
 * Gets a page of manufacturer proposals awaiting review.
 */
fun Route.getOpenManufacturerProposals(
    userManagementService: UserManagementService,
    proposalService: ProposalService
) {
    get("/api/proposals/manufacturers/open") {
        val user = call.getUser(userManagementService, true)
            .getOrNull() ?: return@get

        val offset = call.getPositiveLongParameter(OFFSET_PARAMETER_NAME, 0L)
            ?: return@get call.respond(HttpStatusCode.BadRequest)
        val limit = call.getPositiveLongParameter(LIMIT_PARAMETER_NAME, DEFAULT_LIMIT_SIZE)
            ?.takeIf { it <= Int.MAX_VALUE }
            ?.toInt()
            ?: return@get call.respond(HttpStatusCode.BadRequest)

        proposalService.getOpenManufacturerProposals(user, limit, offset)
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
        summary = "Get open manufacturer proposals"
        description = """
            Gets a page of manufacturer proposals awaiting review.
            Requires a valid User session and the DISPLAY_PROPOSED_MANUFACTURERS authorization.

            Omitted pagination parameters use their default values.
        """.trimIndent()

        parameters {
            query(OFFSET_PARAMETER_NAME) {
                description = "The number of proposals to skip. Must be non-negative. Defaults to 0."
                required = false
                schema = jsonSchema<Long>()
            }
            query(LIMIT_PARAMETER_NAME) {
                description = "The requested maximum number of proposals to return. Must be positive and fit in an Int. " +
                        "Defaults to $DEFAULT_LIMIT_SIZE. " +
                        "The service may enforce a lower maximum."
                required = false
                schema = jsonSchema<Int>()
            }
        }

        responses {
            HttpStatusCode.OK {
                description = "A page of open manufacturer proposals with pagination metadata. Results may be empty."
                schema = jsonSchema<PagedResultDto<UncheckedManufacturerProposalDto>>()
            }
            HttpStatusCode.BadRequest {
                description = "A pagination parameter is invalid or outside its permitted range. No response body."
            }
            HttpStatusCode.Forbidden {
                description = """
                    The session is missing, invalid or expired, or the user lacks the
                    DISPLAY_PROPOSED_MANUFACTURERS authorization.

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