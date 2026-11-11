package org.ouraircraft.api.cookies

import io.ktor.server.application.*
import org.ouraircraft.auth.UserSessionToken
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Sets the secure, HTTP-only cookie identifying a passkey-adding session,
 * with SameSite=Strict.
 *
 * @param userSessionToken The token identifying the session.
 * @param expiresAt The session expiration time used to calculate the cookie's
 * maximum age, or null to create a browser-session cookie.
 */
internal fun ApplicationCall.placeUserSessionCookie(
    userSessionToken: UserSessionToken,
    expiresAt: Instant?
){
    val maxAgeInSeconds = expiresAt?.let { maxOf(0, (it - Clock.System.now()).inWholeSeconds) }
    response.cookies.append(
        name = CookieNames.USER_SESSION,
        value = userSessionToken.toBase64UrlSafe(),
        maxAge = maxAgeInSeconds,
        path = "/",
        httpOnly = true,
        secure = true,
        extensions = CookieExtensions.sameSiteStrict
    )
}

internal fun ApplicationCall.readUserSessionCookie(): UserSessionToken? =
    request.cookies[CookieNames.USER_SESSION]?.let{ cookieValue ->
        try{
            UserSessionToken.fromBase64UrlSafe(cookieValue)
        }
        catch (e: IllegalArgumentException){
            return null
            //TODO custom error, 400 teruggeven.
        }
    }

internal fun ApplicationCall.deleteUserSessionCookie(){
    response.cookies.append(
        name = CookieNames.USER_SESSION,
        value = "",
        maxAge = 0,
        path = "/",
    )
}