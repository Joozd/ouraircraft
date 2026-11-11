package org.ouraircraft.auth.exceptions

/**
 * A login attempt has failed, for example because the passkey does not match or does not exist in the backend.
 */
class BadCredentialsException(reason: String? = null, cause: Throwable? = null): AuthException(reason, cause) {
}