package org.ouraircraft.auth

import kotlinx.io.bytestring.ByteString

/**
 * A toke that identifies a Passkey adding session
 */
class PasskeyAddingSessionToken private constructor(bytes: ByteString): Token(bytes) {
    companion object{
        fun fromBase64UrlSafe(base64: String): PasskeyAddingSessionToken =
            PasskeyAddingSessionToken(bytesFromBase64UrlSafe(base64))

        fun generate(): PasskeyAddingSessionToken {
            return PasskeyAddingSessionToken(generateBytes())
        }
    }
}