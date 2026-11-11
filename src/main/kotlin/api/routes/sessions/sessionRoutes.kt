package org.ouraircraft.api.routes.sessions

import io.ktor.server.routing.*
import org.ouraircraft.model.services.ServiceProvider

fun Route.sessionRoutes(serviceProvider: ServiceProvider){
    createPasskeyAddingSession(serviceProvider.sessionService)
    webAuthnStart(serviceProvider.passkeyCeremonies)
    webAuthnFinish(serviceProvider.passkeyCeremonies, serviceProvider.sessionService)
    me(serviceProvider.sessionService, serviceProvider.userManagementService)
    meRecovery(serviceProvider.sessionService, serviceProvider.userManagementService)
    logout(serviceProvider.sessionService)
}