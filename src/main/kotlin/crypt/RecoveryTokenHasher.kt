package org.ouraircraft.crypt

import org.ouraircraft.auth.util.toByteString
import javax.crypto.Mac
import javax.crypto.SecretKey

/**
 * Computes HMAC-SHA256 hashes of account recovery tokens.
 *
 * Uses a dedicated HMAC key, preferably derived from the application's
 * master key using HKDF with a recovery-token-specific context.
 *
 * @param key The secret HMAC-SHA256 key.
 */
class RecoveryTokenHasher(private val key: SecretKey) {

    /**
     * Computes the HMAC-SHA256 hash of a recovery token.
     *
     * @param token The raw recovery token bytes.
     * @return The resulting 32-byte [HashedToken].
     */
    fun hash(token: ByteArray): HashedToken {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(key)

        return HashedToken(mac.doFinal(token).toByteString())
    }
}
