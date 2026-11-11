package org.ouraircraft.model.services

import kotlinx.io.bytestring.ByteString
import org.ouraircraft.auth.Token
import org.ouraircraft.crypt.EncryptedAesGcmText
import org.ouraircraft.crypt.HashedEmail
import org.ouraircraft.crypt.HashedToken

/**
 * Provides cryptographic operations for email addresses and recovery tokens.
 *
 * Implementations handle key management and cryptographic details internally.
 */
interface CryptoService {

    /** Produces a deterministic HMAC of a recovery token. */
    fun hashToken(token: Token): HashedToken

    /** Produces a deterministic HMAC of a normalized email address. */
    fun hashEmail(email: String): HashedEmail

    /** Encrypts text using authenticated encryption. */
    fun encryptEmail(text: String): EncryptedAesGcmText

    /** Decrypts and authenticates previously encrypted text. */
    fun decryptEmail(encrypted: EncryptedAesGcmText): String

    companion object{
        /**
         * Dummy DOES NOT ENCRYPT JUST STORES AS IS
         */
        fun dummy() = object: CryptoService{
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
    }
}