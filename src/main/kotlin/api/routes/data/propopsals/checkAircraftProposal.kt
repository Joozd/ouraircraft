package org.ouraircraft.api.routes.data.propopsals

import io.ktor.http.*
import io.ktor.openapi.jsonSchema
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.ouraircraft.api.routes.callfunctions.getUser
import org.ouraircraft.auth.exceptions.AuthException
import org.ouraircraft.model.dto.data.CheckResultDto
import org.ouraircraft.model.services.data.ProposalService
import org.ouraircraft.model.services.users.UserManagementService

/**
 * Handles the result of checking an aircraft proposal.
 */
fun Route.checkAircraftProposal(
    userManagementService: UserManagementService,
    proposalService: ProposalService
) {
    post("api/proposals/aircraft/{proposalId}/check") {
        val proposalId = call.parameters["proposalId"]?.toLongOrNull()
            ?: return@post call.respond(HttpStatusCode.BadRequest)
        val user = call.getUser(userManagementService, true)
            .getOrNull() ?: return@post

        val result = call.receive<CheckResultDto>()
        proposalService.handleAircraftProposalCheck(
            user,
            proposalId,
            result.checkResult,
            result.checkResultComment
        )
            .onSuccess {
                call.respond(HttpStatusCode.NoContent)
            }.onFailure { e ->
                if (e is AuthException) {
                    call.respond(HttpStatusCode.Forbidden, "${e::class.simpleName} - ${e.message}")
                } else {
                    throw e // server error is fine for now
                }
            }
    }.describe {
        summary = "Check an aircraft proposal"
        description = """
            Records a decision on an aircraft proposal.
            Requires a valid User session and the DECIDE_ON_PROPOSED_AIRCRAFT authorization.

            If approved, the proposed creation, update or deletion is applied.
            If rejected, the aircraft data remains unchanged.
        """.trimIndent()

        parameters {
            path("proposalId") {
                description = "The ID of the aircraft proposal to check."
                required = true
                schema = jsonSchema<Long>()
            }
        }

        requestBody {
            description = "The decision on the proposal and an optional comment."
            required = true
            schema = jsonSchema<CheckResultDto>()
        }

        responses {
            HttpStatusCode.NoContent {
                description = "The decision was processed successfully. No response body."
            }
            HttpStatusCode.BadRequest {
                description = "The proposalId is missing or is not a valid Long. No response body."
            }
            HttpStatusCode.Forbidden {
                description = """
                    The session is missing, invalid or expired, or the user lacks the
                    DECIDE_ON_PROPOSED_AIRCRAFT authorization.

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