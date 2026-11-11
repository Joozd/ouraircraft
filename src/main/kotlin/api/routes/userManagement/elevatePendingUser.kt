package org.ouraircraft.api.routes.userManagement

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.post
import org.ouraircraft.api.cookies.placePasskeyAddingSessionCookie
import org.ouraircraft.api.routes.sessions.respondToBadLogin
import org.ouraircraft.model.dto.EmailVerificationData
import org.ouraircraft.model.services.SessionService
import org.ouraircraft.model.services.UserManagementService

fun Route.elevatePendingUser(
    userManagementService: UserManagementService,
    sessionService: SessionService
){
    post("/api/confirm-email"){
        val verificationData = call.receive<EmailVerificationData>()
        val recoveryToken = userManagementService.promotePendingUserToUser(verificationData)
            .onFailure { e ->
                call.respondToBadLogin(e)
                return@post
            }
            .getOrThrow()
        // make a passkey adding session
        sessionService.createPasskeyAddingSession(recoveryToken)
            .onSuccess { (sessionData, sessionToken) ->
                call.placePasskeyAddingSessionCookie(
                    sessionToken,
                    sessionData.expiresAt
                )
                call.respond(HttpStatusCode.Created, "Created user.") // frontend should redirect to add passkey page
            }
            .onFailure { e ->
                call.respondToBadLogin(e)
                return@post
            }
    }.describe {
        summary = "Confirm email and prepare passkey registration"
        description = "Validates the email verification data, promotes the pending " +
                "registration to a user account, and creates a passkey-adding session. " +
                "Sets an HTTP-only session cookie that permits passkey registration. " +
                "After a successful response, the frontend should navigate to the " +
                "passkey creation page."

        requestBody {
            description = "The data required to verify the email address."
            required = true
            schema = jsonSchema<EmailVerificationData>()
        }

        responses {
            HttpStatusCode.Created {
                description = "Email confirmed, user account created, and " +
                        "passkey-adding session cookie set."
                ContentType.Text.Plain {
                    schema = jsonSchema<String>()
                }
            }
            HttpStatusCode.Forbidden {
                description = "The verification or recovery token is unknown or expired."
                ContentType.Text.Plain {
                    schema = jsonSchema<String>()
                }
            }
        }
    }
}