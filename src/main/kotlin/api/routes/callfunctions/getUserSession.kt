package org.ouraircraft.api.routes.callfunctions

import io.ktor.server.application.ApplicationCall
import org.ouraircraft.api.cookies.NoSessionCookieFoundException
import org.ouraircraft.api.cookies.readUserSessionCookie
import org.ouraircraft.api.routes.sessions.respondToBadLogin
import org.ouraircraft.model.dto.sessions.UserSessionData
import org.ouraircraft.model.services.users.SessionService

/**
 * Gets a [org.ouraircraft.model.dto.sessions.PasskeyAddingSessionData] object for the current session.
 * If there is no valid current Passkey Adding Session, this returns null.
 * @param sessionService The [SessionService] to use
 * @param sendResponseOnFail if true, this will send a response to the client. See [respondToBadLogin].
 */
internal suspend fun ApplicationCall.getUserSession(
    sessionService: SessionService,
    sendResponseOnFail: Boolean = false
): Result<UserSessionData> =
    (readUserSessionCookie()?.let { token ->
        sessionService.getUserSession(token)
    } ?: Result.failure(NoSessionCookieFoundException()))
        .onFailure { e ->
            if(sendResponseOnFail)
                respondToBadLogin(e)
        }
