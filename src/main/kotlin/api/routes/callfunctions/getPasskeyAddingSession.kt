package org.ouraircraft.api.routes.sessions

import io.ktor.server.application.*
import org.ouraircraft.api.cookies.readPasskeyAddingCookie
import org.ouraircraft.model.dto.sessions.PasskeyAddingSessionData
import org.ouraircraft.model.services.SessionService

/**
 * Gets a [PasskeyAddingSessionData] object for the current session.
 * If there is no valid current Passkey Adding Session, this returns null.
 */
internal suspend fun ApplicationCall.getPasskeyAddingSession(sessionService: SessionService): PasskeyAddingSessionData? =
    readPasskeyAddingCookie()?.let { token ->
        sessionService.getPasskeyAddingSession(token)
            .getOrNull()
    }
