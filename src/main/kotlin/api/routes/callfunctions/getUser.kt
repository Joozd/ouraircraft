package org.ouraircraft.api.routes.callfunctions

import io.ktor.server.application.*
import org.ouraircraft.api.cookies.NoSessionCookieFoundException
import org.ouraircraft.api.cookies.readUserSessionCookie
import org.ouraircraft.api.routes.sessions.respondToBadLogin
import org.ouraircraft.model.auth.User
import org.ouraircraft.model.services.users.UserManagementService


/**
 * Gets the user logged into this session.
 * Will return a Result.Failure if there is no active session.
 */
internal suspend fun ApplicationCall.getUser(
    userManagementService: UserManagementService,
    sendResponseOnFail: Boolean = false
): Result<User> =
    (readUserSessionCookie()?.let { token ->
        userManagementService.getCurrentUserInSession(token)
    } ?: Result.failure(NoSessionCookieFoundException()))
        .onFailure { e ->
            if(sendResponseOnFail)
                respondToBadLogin(e)
        }
