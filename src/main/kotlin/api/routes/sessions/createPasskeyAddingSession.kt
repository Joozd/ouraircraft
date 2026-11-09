package org.ouraircraft.api.routes.userManagement

import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.ouraircraft.auth.RecoveryToken
import org.ouraircraft.model.dto.DtoToken
import org.ouraircraft.services.UserManagementService

/**
 * Create a PasskeyAddingSession using a (serialized) [RecoveryToken]
 */
fun Route.createPasskeyAddingSession(userManagementService: UserManagementService) {
    post("/api/recovery-token") {
        val recoveryToken = RecoveryToken
            .fromBase64UrlSafe(
                call.receive<DtoToken>()
                    .base64UrlSafe
            )

        val sessionToken = userManagementService.createPasskeyAddingSession(recoveryToken)


    }
}