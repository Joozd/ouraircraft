package org.ouraircraft.crypt

import kotlinx.io.bytestring.ByteString
import org.ouraircraft.auth.Token
import org.ouraircraft.model.services.CryptoService

/** Predictable test double; deliberately provides no cryptographic protection. */
internal class TestingCryptoService : CryptoService {
    override fun hashToken(token: Token): HashedToken =
        HashedToken(token.bytes)

    override fun hashEmail(email: String): HashedEmail {
        val bytes = email.trim().lowercase().encodeToByteArray()
        require(bytes.size <= 32) { "TestingCrypto supports email fixtures up to 32 UTF-8 bytes" }
        return HashedEmail(ByteString(bytes.copyOf(32)))
    }

    override fun encryptEmail(text: String): EncryptedAesGcmText =
        EncryptedAesGcmText(text.encodeToByteArray())

    override fun decryptEmail(encrypted: EncryptedAesGcmText): String =
        encrypted.bytes.toByteArray().decodeToString()
}
