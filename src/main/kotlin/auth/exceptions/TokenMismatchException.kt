package org.ouraircraft.auth.exceptions

/**
 * This is thrown when a mismatch is detected between a provided and an actual token
 */
class TokenMismatchException(message: String): AuthException(message)