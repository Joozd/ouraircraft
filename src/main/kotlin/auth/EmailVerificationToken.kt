package org.ouraircraft.auth

import kotlinx.io.bytestring.ByteString

class EmailVerificationToken private constructor(bytes: ByteString): Token(bytes) {
    init{
        require(bytes.size == 32) { "EmailVerificationToken must be 32 bytes, but was ${bytes.size}" }
    }

    companion object{
        fun fromBase64UrlSafe(base64: String): EmailVerificationToken =
            EmailVerificationToken(bytesFromBase64UrlSafe(base64))

        fun generate(): EmailVerificationToken {
            return EmailVerificationToken(generateBytes())
        }
    }
}