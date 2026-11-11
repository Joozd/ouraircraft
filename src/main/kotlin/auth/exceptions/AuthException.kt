package org.ouraircraft.auth.exceptions

/**
 * An exception thrown by the auth system, such as a [TokenMismatchException]
 */
abstract class AuthException(message: String? = null, cause: Throwable? = null): IllegalStateException(message, cause)