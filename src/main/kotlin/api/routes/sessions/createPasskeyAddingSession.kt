package org.ouraircraft.api.routes.sessions

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.request.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.describe
import org.ouraircraft.api.cookies.placePasskeyAddingSessionCookie
import org.ouraircraft.auth.RecoveryToken
import org.ouraircraft.auth.exceptions.TokenNotFoundException
import org.ouraircraft.model.dto.DtoToken
import org.ouraircraft.model.services.users.SessionService

/**
 * Registers an endpoint that exchanges a recovery token for a passkey-adding
 * session and sets its HTTP-only session cookie.
 *
 * Responds with 204 on success or 400 if the token is malformed, unknown,
 * or expired. Other service failures propagate to the application's error handler.
 *
 * @param sessionService The service responsible for creating the session.
 */
fun Route.createPasskeyAddingSession(sessionService: SessionService) {
    post("/api/recovery-token") {
        val tokenData = call.receive<DtoToken>()
        val recoveryToken = try {
            RecoveryToken.fromBase64UrlSafe(tokenData.base64UrlSafe)
        } catch (_: IllegalArgumentException) {
            call.respond(
                HttpStatusCode.BadRequest,
                "Invalid recovery token. Please start a new recovery."
            )
            return@post
        }

        sessionService.createPasskeyAddingSession(recoveryToken)
            .onSuccess { (sessionData, sessionToken) ->
                call.placePasskeyAddingSessionCookie(
                    sessionToken,
                    sessionData.expiresAt
                )
                call.respond(HttpStatusCode.NoContent)
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
    }.describe {
        summary = "Create a passkey-adding session"
        description = "Exchanges a recovery token for a session that permits " +
                "adding a passkey. Sets the session cookie; the frontend handles navigation."

        requestBody {
            description = "The recovery token encoded as URL-safe Base64."
            required = true
            schema = jsonSchema<DtoToken>()
        }

        responses {
            HttpStatusCode.NoContent {
                description = "Session created and its cookie set. No response body."
            }
            HttpStatusCode.BadRequest {
                description = "The recovery token is malformed, unknown, or expired."
                ContentType.Text.Plain {
                    schema = jsonSchema<String>()
                }
            }
        }
    }
}