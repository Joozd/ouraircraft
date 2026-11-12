package org.ouraircraft.api.routes.sessions

import io.ktor.http.*
import io.ktor.openapi.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import org.ouraircraft.model.dto.PasskeyRequest
import org.ouraircraft.model.services.users.PasskeyCeremonies

fun Route.webAuthnStart(passkeyCeremonies: PasskeyCeremonies) {
    post("/api/users/webauthn/start") {
        val passkeyRequest: PasskeyRequest = passkeyCeremonies.generateAuthnRequestJson()
        call.respond(HttpStatusCode.OK, passkeyRequest)
    }.describe {
        summary = "Start passkey authentication"
        description = """
        Starts a passkey authentication attempt without requiring a username.
        
        Returns a PasskeyRequest containing:
        - id: identifies this authentication attempt.
        - requestJson: WebAuthn request options serialized as a JSON string.
        
        Parse requestJson as JSON and pass the resulting object unchanged to
        get() from @github/webauthn-json.
        
        After the ceremony succeeds, POST a PasskeyResponse to
        /api/users/webauthn/finish:
        - id: the id in the PasskeyRequest returned by this endpoint.
        - responseJson: JSON.stringify() applied to the credential returned by get().
    """.trimIndent()

        responses {
            HttpStatusCode.OK {
                description = "The authentication attempt ID and WebAuthn request options."
                ContentType.Application.Json {
                    schema = jsonSchema<PasskeyRequest>()
                }
            }
        }
    }
}