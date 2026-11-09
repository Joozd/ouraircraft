package org.ouraircraft.crypt

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
    fun encrypt(plaintext: ByteArray): ByteArray {
        val nonce = ByteArray(12).also(random::nextBytes)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            key,
            GCMParameterSpec(128, nonce)
        )

        return nonce + cipher.doFinal(plaintext)
    }

    /**
     * Decrypts data and verifies its authentication tag.
     *
     * Throws AEADBadTagException if authentication fails.
     */
    fun decrypt(encrypted: ByteArray): ByteArray {
        require(encrypted.size >= 28) {
            "Invalid encrypted data"
        }

        val nonce = encrypted.copyOfRange(0, 12)
        val ciphertext = encrypted.copyOfRange(12, encrypted.size)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            GCMParameterSpec(128, nonce)
        )

        return cipher.doFinal(ciphertext)
    }
}