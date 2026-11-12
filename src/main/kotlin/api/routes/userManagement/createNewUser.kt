package org.ouraircraft.api.routes.userManagement

import io.ktor.http.*
import io.ktor.openapi.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import org.ouraircraft.api.ApiErrorCodes
import org.ouraircraft.db.exceptions.DuplicateEntryException
import org.ouraircraft.db.exceptions.EmailAlreadyInUseException
import org.ouraircraft.db.exceptions.UserAlreadyExistsException
import org.ouraircraft.model.dto.ApiError
import org.ouraircraft.model.dto.NewUserRegistrationData
import org.ouraircraft.model.services.users.UserManagementService

/**
 * Creates a new pending user registration and returns a link with its activation token.
 */
fun Route.createNewUser(userManagementService: UserManagementService) {
    post("/api/users") {
        val registrationData = call.receive<NewUserRegistrationData>()

        userManagementService.createNewPendingUserRegistration(registrationData)
            .onSuccess { activationLink ->
                call.respondText(
                    activationLink.toString(),
                    ContentType.Text.Plain,
                    HttpStatusCode.Created
                )
                //TODO when email is up email that instead of returning the link
            }
            .onFailure {
                if (it is DuplicateEntryException) {
                    val error = makeConflictApiError(it)
                    call.respond(HttpStatusCode.Conflict, error)
                }
                else throw it
            }
    }.describe {
        summary = "Create a new user"
        description = "Creates a pending user registration."

        requestBody {
            required = true
            schema = jsonSchema<NewUserRegistrationData>()
        }

        responses {
            HttpStatusCode.Created {
                description = "A pending registration was created. Returns the email verification URL."
                ContentType.Text.Plain()
            }

            HttpStatusCode.Conflict {
                description = """
            Registration conflicts with an existing entry.

            Possible error codes:
            - ${ApiErrorCodes.USERNAME_TAKEN}: The username is already in use.
            - ${ApiErrorCodes.EMAIL_ALREADY_IN_USE}: The email address is already in use.
            - ${ApiErrorCodes.DUPLICATE_ENTRY}: An unspecified duplicate entry was detected.
        """.trimIndent()

                schema = jsonSchema<ApiError>()
            }
        }
    }
}

/**
 * Makes an [ApiError] from a domain [DuplicateEntryException]
 */
private fun makeConflictApiError(error: DuplicateEntryException): ApiError =
    when(error) {
        is UserAlreadyExistsException -> ApiError(ApiErrorCodes.USERNAME_TAKEN, "username is already taken")
        is EmailAlreadyInUseException -> ApiError(ApiErrorCodes.EMAIL_ALREADY_IN_USE, "email is already taken")
        else -> ApiError(ApiErrorCodes.DUPLICATE_ENTRY, "An unknown duplicate entry code occurred")
    }