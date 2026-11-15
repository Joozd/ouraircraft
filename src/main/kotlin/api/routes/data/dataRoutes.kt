package org.ouraircraft.api.routes.data

import io.ktor.server.routing.*
import org.ouraircraft.api.routes.data.propopsals.*
import org.ouraircraft.model.services.ServiceProvider

/**
 * Registers proposal routes for manufacturers, aircraft types, and aircraft.
 */
fun Route.dataRoutes(serviceProvider: ServiceProvider) {
    createNewManufacturerProposal(serviceProvider.userManagementService, serviceProvider.proposalService)
    createUpdateManufacturerProposal(serviceProvider.userManagementService, serviceProvider.proposalService)
    createDeleteManufacturerProposal(serviceProvider.userManagementService, serviceProvider.proposalService)

    createNewAircraftTypeProposal(serviceProvider.userManagementService, serviceProvider.proposalService)
    createUpdateAircraftTypeProposal(serviceProvider.userManagementService, serviceProvider.proposalService)
    createDeleteAircraftTypeProposal(serviceProvider.userManagementService, serviceProvider.proposalService)

    createNewAircraftProposal(serviceProvider.userManagementService, serviceProvider.proposalService)
    createUpdateAircraftProposal(serviceProvider.userManagementService, serviceProvider.proposalService)
    createDeleteAircraftProposal(serviceProvider.userManagementService, serviceProvider.proposalService)
}
