package org.ouraircraft.api.routes.userManagement

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import io.ktor.openapi.JsonSchema
import io.ktor.openapi.JsonType
import io.ktor.openapi.jsonSchema
import org.ouraircraft.api.routes.callfunctions.getPasskeyAddingSession
import org.ouraircraft.model.services.users.PasskeyCeremonies
import org.ouraircraft.model.services.users.SessionService


/**
 * Starts passkey registration for a user with a valid passkey-adding session.
 */
fun Route.registerPasskeyStart(sessionService: SessionService, passkeyCeremonies: PasskeyCeremonies) {
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
    }.describe {
        summary = "Start passkey registration"
        description = """
        Requires a valid passkey-adding session cookie.
        
        Returns WebAuthn creation options as a JSON object, including the publicKey wrapper.
        Parse the response as JSON and pass the complete object unchanged to
        create() from @github/webauthn-json.
        
        After the WebAuthn ceremony succeeds, POST a JSON object to
        /api/passkeys/register/finish with:
        - label: a user-chosen name for the passkey.
        - passkeyRegistrationResponseJson: JSON.stringify() applied to the credential
          returned by create(). This field must contain a JSON string, not a nested object.
        
        Binary values in the creation options are Base64url encoded.
        This endpoint does not yet store a registered passkey.
    """.trimIndent()

        responses {
            HttpStatusCode.OK {
                description = "WebAuthn creation options for registering a passkey."
                ContentType.Application.Json {
                    schema = JsonSchema(type = JsonType.OBJECT)
                }
            }
            HttpStatusCode.Forbidden {
                description = "The passkey-adding session cookie is missing, invalid or expired."
                ContentType.Text.Plain {
                    schema = jsonSchema<String>()
                }
            }
        }
    }
}