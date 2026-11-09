package org.ouraircraft.api.routes.userManagement

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.ouraircraft.api.routes.sessions.getPasskeyAddingSession
import org.ouraircraft.model.services.PasskeyCeremonies
import org.ouraircraft.model.services.SessionService

/**
 * Starts passkey registration for a user with a valid passkey-adding session.
 */
fun Route.registerPasskey(sessionService: SessionService, passkeyCeremonies: PasskeyCeremonies) {
    post("/api/passkeys/register/start") {
        val sessionData = call.getPasskeyAddingSession(sessionService)
            //no valid PasskeyAddingSession:
            ?: return@post call.respond(HttpStatusCode.Forbidden, "There is no valid passkey adding session")

        val passkeyRegistrationJson = passkeyCeremonies.makePasskeyCreationOptionsJsonString(sessionData.userId)

        call.respondText(
            passkeyRegistrationJson,
            ContentType.Application.Json,
            HttpStatusCode.OK
        )
    }
}