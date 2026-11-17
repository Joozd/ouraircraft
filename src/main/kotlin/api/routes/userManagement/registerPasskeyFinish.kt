package org.ouraircraft.api.routes.userManagement

import io.ktor.http.*
import io.ktor.openapi.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import org.apache.logging.log4j.LogManager
import org.ouraircraft.api.cookies.deletePasskeyAddingCookie
import org.ouraircraft.api.cookies.placeUserSessionCookie
import org.ouraircraft.api.routes.callfunctions.getPasskeyAddingSession
import org.ouraircraft.auth.exceptions.RegistrationFailedException
import org.ouraircraft.auth.exceptions.TokenNotFoundException
import org.ouraircraft.model.dto.PasskeyRegistrationResponse
import org.ouraircraft.model.services.users.PasskeyCeremonies
import org.ouraircraft.model.services.users.SessionService

private val logger = LogManager.getLogger("Route.registerPasskeyFinish")

/**
 * Verifies and stores a new passkey for a user with a valid passkey-adding session.
 */
fun Route.registerPasskeyFinish(sessionService: SessionService, passkeyCeremonies: PasskeyCeremonies) {
    post("/api/passkeys/register/finish") {
        logger.debug("registerPasskeyFinish started")
        val passkeyAddingSessionData = call.getPasskeyAddingSession(sessionService)
            .also { logger.debug(it)}

        //no valid PasskeyAddingSession:
            ?: return@post call.respond(HttpStatusCode.Forbidden, "There is no valid passkey adding session")

        val passkeyRegistrationResponse = call.receive<PasskeyRegistrationResponse>()

        val passkeyData = try {
            passkeyCeremonies.handlePasskeyRegistrationResponse(passkeyAddingSessionData.userId, passkeyRegistrationResponse)
        } catch (e: RegistrationFailedException) {
            call.respond(HttpStatusCode.BadRequest, "Registration failed. Given reason:  ${e.message}") // TODO filter out server side and client side problems, MVP for now.
            return@post
        }


        /*
         * register the session and place cookie
         */
        sessionService.createUserSession(passkeyData)
            .onSuccess { (userSessionData, sessionToken) ->
                call.placeUserSessionCookie(sessionToken,userSessionData.expiresAt)
                call.deletePasskeyAddingCookie()
                sessionService.invalidatePasskeyAddingSession(passkeyAddingSessionData)
            }
            .onFailure { e ->
                when (e) {
                    is TokenNotFoundException -> call.respond(
                        HttpStatusCode.BadRequest,
                        "Recovery token not found or expired. Please start a new recovery."
                    )
                    else -> throw e
                }
            }


        call.respond(HttpStatusCode.Created)
    }.describe {
        summary = "Finish passkey registration"
        description = """
        Requires a valid passkey-adding session cookie and a pending registration
        started through POST /api/passkeys/register/start.
        
        Verifies the WebAuthn registration response and stores the new passkey.
        If the passkey is successfully created, this also created a UserSession, logging the user in
        and storing a cookie with the session token on the client.
        
        The request contains:
        - label: a user-chosen name for this passkey.
        - passkeyRegistrationResponseJson: the result of JSON.stringify() applied
          to the credential returned by create() from @github/webauthn-json.
          This field contains a JSON string, not a nested object.
    """.trimIndent()

        requestBody {
            required = true
            description = "The passkey label and serialized WebAuthn registration response."
            ContentType.Application.Json {
                schema = jsonSchema<PasskeyRegistrationResponse>()
            }
        }

        responses {
            HttpStatusCode.Created {
                description = "The passkey was registered successfully. No response body."
            }
            HttpStatusCode.BadRequest {
                description = "WebAuthn registration verification failed."
            }
            HttpStatusCode.Forbidden {
                description = "The passkey-adding session cookie is missing, invalid or expired."
                ContentType.Text.Plain {
                    schema = jsonSchema<String>()
                }
            }
            HttpStatusCode.InternalServerError {
                description = "An unhandled server error occurred"
            }
        }
    }
}