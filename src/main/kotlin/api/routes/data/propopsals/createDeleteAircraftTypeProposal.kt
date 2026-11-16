package org.ouraircraft.api.routes.data.propopsals

import io.ktor.http.*
import io.ktor.openapi.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import org.ouraircraft.api.routes.callfunctions.getUser
import org.ouraircraft.auth.exceptions.AuthException
import org.ouraircraft.model.dto.convertors.toAircraftTypeProposal
import org.ouraircraft.model.dto.data.DeleteAircraftTypeProposalDto
import org.ouraircraft.model.services.data.ProposalService
import org.ouraircraft.model.services.users.UserManagementService

/**
 * Creates a proposal to delete an existing aircraft type.
 */
fun Route.createDeleteAircraftTypeProposal(
    userManagementService: UserManagementService,
    proposalService: ProposalService
) {
    post("api/proposals/aircraftTypes/{targetId}/delete") {
        val targetId = call.parameters["targetId"]?.toLongOrNull()
            ?: return@post call.respond(HttpStatusCode.BadRequest)
        val user = call.getUser(userManagementService, true)
            .getOrNull() ?: return@post

        val proposal = call.receive<DeleteAircraftTypeProposalDto>().toAircraftTypeProposal(user, targetId)

        proposalService.proposeAircraftType(user, proposal)
            .onSuccess {
                call.respond(HttpStatusCode.Created)
            }.onFailure { e ->
                if (e is AuthException) {
                    call.respond(HttpStatusCode.Forbidden, "${e::class.simpleName} - ${e.message}")
                } else {
                    throw e // server error is fine for now
                }
            }
    }.describe {
        summary = "Propose deleting an aircraft type"
        description = """
            Creates a proposal to delete an existing aircraft type.
            Requires a valid User session and PROPOSE_DELETE_AIRCRAFT_TYPES authorization.

            The aircraft type is only deleted after the proposal is approved.
        """.trimIndent()

        parameters {
            path("targetId") {
                description = "The ID of the aircraft type proposed for deletion."
                required = true
                schema = jsonSchema<Long>()
            }
        }

        requestBody {
            description = "The aircraft type deletion proposal data."
            required = true
            schema = jsonSchema<DeleteAircraftTypeProposalDto>()
        }

        responses {
            HttpStatusCode.Created {
                description = "The deletion proposal was created successfully. No response body."
            }
            HttpStatusCode.BadRequest {
                description = "The targetId is missing or is not a valid Long. No response body."
            }
            HttpStatusCode.Forbidden {
                description = "The User session is missing, invalid or expired, or the user is not authorized to perform this action."
                ContentType.Text.Plain()
            }
            HttpStatusCode.InternalServerError {
                description = "An unhandled server error occurred."
            }
        }
    }
}