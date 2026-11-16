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
import org.ouraircraft.model.dto.data.NewAircraftTypeProposalDto
import org.ouraircraft.model.services.data.ProposalService
import org.ouraircraft.model.services.users.UserManagementService

/**
 * Creates a proposal to create a new aircraft type.
 */
fun Route.createNewAircraftTypeProposal(
    userManagementService: UserManagementService,
    proposalService: ProposalService
) {
    post("api/proposals/aircraftTypes") {
        val user = call.getUser(userManagementService, true)
            .getOrNull() ?: return@post

        val proposal = call.receive<NewAircraftTypeProposalDto>().toAircraftTypeProposal(user)

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
        summary = "Propose a new aircraft type"
        description = """
            Creates a proposal to add a new aircraft type.
            Requires a valid User session and PROPOSE_AIRCRAFT_TYPES authorization.

            The aircraft type is only created after the proposal is approved.
        """.trimIndent()

        requestBody {
            description = "The proposed aircraft type data."
            required = true
            schema = jsonSchema<NewAircraftTypeProposalDto>()
        }

        responses {
            HttpStatusCode.Created {
                description = "The proposal was created successfully. No response body."
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