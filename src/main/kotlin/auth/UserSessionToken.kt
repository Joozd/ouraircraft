package org.ouraircraft.auth

import kotlinx.io.bytestring.ByteString

/**
 * A toke that identifies a Passkey adding session
 */
class UserSessionToken private constructor(bytes: ByteString): Token(bytes) {
    companion object{
        fun fromBase64UrlSafe(base64: String): UserSessionToken =
            UserSessionToken(bytesFromBase64UrlSafe(base64))

        fun generate(): UserSessionToken {
            return UserSessionToken(generateBytes())
        }
    }
}