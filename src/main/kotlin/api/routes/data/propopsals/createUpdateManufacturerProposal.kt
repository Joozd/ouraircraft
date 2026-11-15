package org.ouraircraft.api.routes.data.propopsals

import io.ktor.http.*
import io.ktor.openapi.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import org.ouraircraft.api.routes.callfunctions.getUser
import org.ouraircraft.auth.exceptions.AuthException
import org.ouraircraft.model.dto.convertors.toManufacturerProposal
import org.ouraircraft.model.dto.data.NewManufacturerProposalDto
import org.ouraircraft.model.services.data.ProposalService
import org.ouraircraft.model.services.users.UserManagementService

/**
 * Create a proposal to update an existing manufacturer.
 */
fun Route.createUpdateManufacturerProposal(
    userManagementService: UserManagementService,
    proposalService: ProposalService
){
    post("api/proposals/manufacturers/{targetId}"){
        val targetId = call.parameters["targetId"]?.toLongOrNull()
            ?: return@post call.respond(HttpStatusCode.BadRequest)
        val user = call.getUser(userManagementService)
            .getOrNull() ?: return@post

        val proposal = call.receive<NewManufacturerProposalDto>().toManufacturerProposal(user, targetId)

        proposalService.proposeManufacturer(user, proposal)
            .onSuccess {
                call.respond(HttpStatusCode.Created)
            }.onFailure { e->
                if (e is AuthException){
                    call.respond(HttpStatusCode.Forbidden, "${e::class.simpleName} - ${e.message}")
                }
                else throw(e) // server error is fine for now

            }
    }.describe {
        summary = "Propose an update to an existing manufacturer"
        description = """
        Creates a proposal to update an existing manufacturer.
        Requires a valid User session and PROPOSE_MANUFACTURERS authorization.
        
        The manufacturer is only updated after the proposal is approved.
    """.trimIndent()

        requestBody {
            description = "The proposed manufacturer data."
            required = true
            schema = jsonSchema<NewManufacturerProposalDto>()
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