package org.ouraircraft.auth

import kotlinx.io.bytestring.ByteString
import java.security.SecureRandom
import kotlin.io.encoding.Base64

abstract class Token(val bytes: ByteString) {
    init{
        require(bytes.size == 32) { "A token must be 32 bytes, but was ${bytes.size}" }
    }
    fun toBase64UrlSafe(): String =
        Base64.UrlSafe
            .withPadding(Base64.PaddingOption.ABSENT)
            .encode(bytes.toByteArray())

    companion object{
        private val random = SecureRandom()

        @JvmStatic
        protected fun bytesFromBase64UrlSafe(base64: String): ByteString {
            val bytes = Base64.UrlSafe
                .withPadding(Base64.PaddingOption.ABSENT)
                .decode(base64)
            return ByteString(bytes)
        }

        @JvmStatic
        protected fun generateBytes(): ByteString{
            val bytes = ByteArray(32).apply{
                random.nextBytes(this)
            }
            return ByteString(bytes)
        }
    }
}