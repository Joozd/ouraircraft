package org.ouraircraft.auth.exceptions

/**
 * This is thrown when a token is not found or is expired.
 */
class TokenNotFoundException(message: String?): AuthException(message)