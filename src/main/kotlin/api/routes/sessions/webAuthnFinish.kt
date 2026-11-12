package org.ouraircraft.api.routes.sessions

import io.ktor.http.*
import io.ktor.openapi.jsonSchema
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.ouraircraft.api.cookies.placeUserSessionCookie
import org.ouraircraft.auth.exceptions.AuthException
import org.ouraircraft.auth.exceptions.BadCredentialsException
import org.ouraircraft.auth.exceptions.PasskeyCeremonyNotFoundException
import org.ouraircraft.model.dto.PasskeyResponse
import org.ouraircraft.model.services.users.PasskeyCeremonies
import org.ouraircraft.model.services.users.SessionService

/**
 * Completes a passkey authentication ceremony and creates a user session.
 */
fun Route.webAuthnFinish(passkeyCeremonies: PasskeyCeremonies, sessionService: SessionService){
    post("/api/users/webauthn/finish") {
        val response = call.receive<PasskeyResponse>()
        val passkeyData = try {
            passkeyCeremonies.handleAuthnRequestResponse(response)
        } catch (e: AuthException) {
            when (e) {
                is BadCredentialsException -> call.respond(HttpStatusCode.BadRequest, "bad login data provided")
                is PasskeyCeremonyNotFoundException -> call.respond(HttpStatusCode.NotFound, "This login ceremony is not found, try again from the start")
                else -> throw e
            }
            return@post
        }
        sessionService.createUserSession(passkeyData)
            .onSuccess { (sessionData, token) ->
                call.placeUserSessionCookie(token, sessionData.expiresAt)
                call.respond(HttpStatusCode.NoContent)
            }
            .onFailure {
                throw(it)
                //TODO handle failure
            }
    }.describe {
        summary = "Finish passkey authentication"
        description = """
            Completes a passkey authentication attempt started through
            POST /api/users/webauthn/start.

            The request must contain the attempt id returned by the start endpoint
            and the JSON-stringified credential returned by
            @github/webauthn-json.get().

            On successful authentication, creates a user session and sets
            the session cookie.
        """.trimIndent()

        requestBody {
            required = true
            description = "The authentication attempt ID and serialized WebAuthn response."
            ContentType.Application.Json {
                schema = jsonSchema<PasskeyResponse>()
            }
        }

        responses {
            HttpStatusCode.NoContent {
                description = "Authentication succeeded and the user session cookie was set."
            }
            HttpStatusCode.BadRequest {
                description = "The provided login data was invalid."
                ContentType.Text.Plain {
                    schema = jsonSchema<String>()
                }
            }
            HttpStatusCode.NotFound {
                description = "The authentication attempt was not found or has expired."
                ContentType.Text.Plain {
                    schema = jsonSchema<String>()
                }
            }
        }
    }
}