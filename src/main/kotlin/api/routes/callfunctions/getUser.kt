package org.ouraircraft.api.routes.callfunctions

import io.ktor.server.application.*
import org.ouraircraft.model.auth.User
import org.ouraircraft.model.services.users.SessionService
import org.ouraircraft.model.services.users.UserManagementService


/**
 * Gets the user logged into this session.
 * Will return a Result.Failure if there is no active session.
 */
internal suspend fun ApplicationCall.getUser(
    sessionService: SessionService,
    userManagementService: UserManagementService,
    sendResponseOnFail: Boolean = false
): Result<User>{
    val sessionData = getUserSession(sessionService, sendResponseOnFail)
        .onFailure { return Result.failure(it) }
        .getOrThrow()

    return userManagementService.getUserById(sessionData.userId)
}