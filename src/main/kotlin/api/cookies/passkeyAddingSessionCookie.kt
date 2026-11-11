package org.ouraircraft.api.cookies

import io.ktor.server.application.*
import org.ouraircraft.auth.PasskeyAddingSessionToken
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Sets the secure, HTTP-only cookie identifying a passkey-adding session,
 * with SameSite=Strict.
 *
 * @param passkeyAddingSessionToken The token identifying the session.
 * @param expiresAt The session expiration time used to calculate the cookie's
 * maximum age, or null to create a browser-session cookie.
 */
internal fun ApplicationCall.placePasskeyAddingSessionCookie(
    passkeyAddingSessionToken: PasskeyAddingSessionToken,
    expiresAt: Instant?
){
    val maxAgeInSeconds = expiresAt?.let { maxOf(0, (it - Clock.System.now()).inWholeSeconds) }
    response.cookies.append(
        name = CookieNames.PASSKEY_ADDING_SESSION,
        value = passkeyAddingSessionToken.toBase64UrlSafe(),
        maxAge = maxAgeInSeconds,
        path = "/",
        httpOnly = true,
        secure = true,
        extensions = CookieExtensions.sameSiteStrict
    )
}

internal fun ApplicationCall.readPasskeyAddingCookie(): PasskeyAddingSessionToken? =
    request.cookies[CookieNames.PASSKEY_ADDING_SESSION]?.let{ cookieValue ->
        try{
            PasskeyAddingSessionToken.fromBase64UrlSafe(cookieValue)
        }
        catch (e: IllegalArgumentException){
            return null
            //TODO custom error, 400 teruggeven.
        }
    }

internal fun ApplicationCall.deletePasskeyAddingCookie(){
    response.cookies.append(
        name = CookieNames.PASSKEY_ADDING_SESSION,
        value = "",
        maxAge = 0,
        path = "/",
    )
}