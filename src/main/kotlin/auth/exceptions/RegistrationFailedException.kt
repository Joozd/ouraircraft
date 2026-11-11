package org.ouraircraft.auth.exceptions

class RegistrationFailedException(reason: String? = null, cause: Throwable? = null): IllegalStateException(reason, cause) {
}