package org.ouraircraft.api.routes.data.propopsals

import io.ktor.server.routing.Route
import org.ouraircraft.model.services.data.ProposalService
import org.ouraircraft.model.services.users.UserManagementService

internal fun Route.proposalRoutes(
    userManagementService: UserManagementService,
    proposalService: ProposalService
){
    createNewManufacturerProposal(userManagementService, proposalService)
    createUpdateManufacturerProposal(userManagementService, proposalService)
    createDeleteManufacturerProposal(userManagementService, proposalService)
    getOpenManufacturerProposals(userManagementService, proposalService)
    checkManufacturerProposal(userManagementService, proposalService)

    createNewAircraftTypeProposal(userManagementService, proposalService)
    createUpdateAircraftTypeProposal(userManagementService, proposalService)
    createDeleteAircraftTypeProposal(userManagementService, proposalService)
    getOpenAircraftTypeProposals(userManagementService, proposalService)
    checkAircraftTypeProposal(userManagementService, proposalService)

    createNewAircraftProposal(userManagementService, proposalService)
    createUpdateAircraftProposal(userManagementService, proposalService)
    createDeleteAircraftProposal(userManagementService, proposalService)
    getOpenAircraftProposals(userManagementService, proposalService)
    checkAircraftProposal(userManagementService, proposalService)
}