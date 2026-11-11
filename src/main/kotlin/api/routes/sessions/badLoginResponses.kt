package org.ouraircraft.api.routes.sessions

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import org.ouraircraft.api.cookies.NoSessionCookieFoundException
import org.ouraircraft.auth.exceptions.TokenNotFoundException

suspend fun ApplicationCall.respondToBadLogin(e: Throwable){
    when(e){
        is TokenNotFoundException -> respond(HttpStatusCode.Forbidden, "Bad login information provided. Please re-login.")
        is NoSessionCookieFoundException -> respond(HttpStatusCode.Forbidden, "No stored session found. Please log in.")
        else -> throw e // expand this when needed
    }
}