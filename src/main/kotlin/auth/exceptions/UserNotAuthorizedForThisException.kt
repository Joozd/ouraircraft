package org.ouraircraft.auth.exceptions

class UserNotAuthorizedForThisException(message: String? = null, cause: Throwable? = null): AuthException(message, cause) {
}