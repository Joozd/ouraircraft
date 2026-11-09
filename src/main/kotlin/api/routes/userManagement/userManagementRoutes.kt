package org.ouraircraft.api.routes.userManagement

import io.ktor.server.routing.Route
import org.ouraircraft.api.routes.sessions.createPasskeyAddingSession
import org.ouraircraft.services.ServiceProvider

fun Route.userManagement(serviceProvider: ServiceProvider){
    checkScreenNameAvailable(serviceProvider.userManagementService)
    createNewUser(serviceProvider.userManagementService)



    createPasskeyAddingSession(serviceProvider.userManagementService)



}