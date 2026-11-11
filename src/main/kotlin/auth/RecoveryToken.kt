package org.ouraircraft.auth

import kotlinx.io.bytestring.ByteString

class RecoveryToken private constructor(bytes: ByteString): Token(bytes) {
    init{
        require(bytes.size == 32) { "RecoveryToken must be 32 bytes, but was ${bytes.size}" }
    }

    companion object{
        fun fromBase64UrlSafe(base64: String): RecoveryToken =
            RecoveryToken(bytesFromBase64UrlSafe(base64))

        fun generate(): RecoveryToken {
            return RecoveryToken(generateBytes())
        }
    }
}