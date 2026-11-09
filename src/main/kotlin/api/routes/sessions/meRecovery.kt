package org.ouraircraft.api.routes.sessions

import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.ouraircraft.api.cookies.readPasskeyAddingCookie
import org.ouraircraft.services.SessionService

fun Route.passkeyRecoverySessionValid(sessionService: SessionService){
    get("/api/recovery-session-valid"){
        val token = call.readPasskeyAddingCookie()

    }
}