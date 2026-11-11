package org.ouraircraft.api.cookies

/**
 * A Session cookie was needed, but not found
 */
class NoSessionCookieFoundException(message: String? = null): IllegalStateException(message) {
}