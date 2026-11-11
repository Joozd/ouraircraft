package org.ouraircraft.auth.exceptions

/**
 * A passkey ceremony is being completed, but there is no information on how is started.
 * This can happen when it times out (after a set amount of time the backend clears up any open login attempts)
 * or when data is sent to the `finish` endpoint without a completed `start`.
 */
class PasskeyCeremonyNotFoundException(reason: String? = null): AuthException(reason) {
}