package org.ouraircraft.api.routes.data

import io.ktor.server.routing.*
import org.ouraircraft.api.routes.data.live.searchRoutes
import org.ouraircraft.api.routes.data.propopsals.*
import org.ouraircraft.model.services.ServiceProvider

/**
 * Registers routes for the actual OurAircraft.org data.
 */
fun Route.dataRoutes(serviceProvider: ServiceProvider) {
    proposalRoutes(serviceProvider.userManagementService, serviceProvider.proposalService)
    searchRoutes(serviceProvider.userManagementService, serviceProvider.dataRetrievalService)
}
