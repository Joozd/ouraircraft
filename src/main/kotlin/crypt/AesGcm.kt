package org.ouraircraft.crypt

import kotlinx.io.bytestring.ByteString
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts and decrypts data using AES-256-GCM.
 *
 * The encrypted output consists of:
 * - 12 bytes: random nonce
 * - N bytes: ciphertext
 * - 16 bytes: authentication tag
 *
 * A fresh nonce is generated for every encryption.
 */
class AesGcm(private val key: SecretKey) {
    private val random = SecureRandom()

    init {
        require(key.algorithm == "AES" && key.encoded?.size == 32) {
            "A 256-bit AES key is required"
        }
    }

    /** Encrypts data, prepending the randomly generated nonce. */
    fun encrypt(plaintext: ByteArray): EncryptedAesGcm {
        val nonce = ByteArray(12).also(random::nextBytes)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            key,
            GCMParameterSpec(128, nonce)
        )

        val bytes = ByteString(
            nonce + cipher.doFinal(plaintext)
        )

        return EncryptedAesGcm(bytes)
    }

    fun encryptText(plaintext: String): EncryptedAesGcmText {
        val encrypted = encrypt(plaintext.toByteArray(Charsets.UTF_8))
        return EncryptedAesGcmText(encrypted.bytes)
    }

    /**
     * Decrypts data and verifies its authentication tag.
     *
     * Throws AEADBadTagException if authentication fails.
     */
    fun decrypt(encrypted: EncryptedAesGcm): ByteString {
        val encryptedBytes = encrypted.bytes.toByteArray()
        require(encryptedBytes.size >= 28) {
            "Invalid encrypted data"
        }

        val nonce = encryptedBytes.copyOfRange(0, 12)
        val ciphertext = encryptedBytes.copyOfRange(12, encryptedBytes.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            GCMParameterSpec(128, nonce)
        )

        return ByteString(cipher.doFinal(ciphertext))
    }

    fun decryptText(encrypted: EncryptedAesGcmText): String {
        val decryptedBytes = decrypt(EncryptedAesGcm(encrypted.bytes))
        return decryptedBytes.toByteArray().toString(Charsets.UTF_8)
    }
}