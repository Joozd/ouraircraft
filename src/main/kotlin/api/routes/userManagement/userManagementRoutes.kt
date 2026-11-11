package org.ouraircraft.api.routes.userManagement

import io.ktor.server.routing.Route
import org.ouraircraft.model.services.ServiceProvider

fun Route.userManagementRoutes(serviceProvider: ServiceProvider){
    checkScreenNameAvailable(serviceProvider.userManagementService)
    createNewUser(serviceProvider.userManagementService)
    elevatePendingUser(serviceProvider.userManagementService, serviceProvider.sessionService)
    registerPasskeyStart(serviceProvider.sessionService, serviceProvider.passkeyCeremonies)
    registerPasskeyFinish(serviceProvider.sessionService, serviceProvider.passkeyCeremonies)
}