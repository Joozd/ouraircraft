package org.ouraircraft.crypt

import kotlinx.io.bytestring.ByteString
import javax.crypto.Mac
import javax.crypto.SecretKey

/**
 * Generates deterministic, keyed SHA-256 hashes.
 *
 * The key must be dedicated to HMAC-SHA256,
 * preferably derived from the master key using HKDF.
 */
class EmailHasher(private val key: SecretKey) {

    /** Returns the 32-byte HMAC of a normalized email address. */
    fun hash(email: String): HashedEmail {
        val normalized = email.trim().lowercase()

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(key)

        val bytes = ByteString(
            mac.doFinal(normalized.toByteArray(Charsets.UTF_8))
        )

        return HashedEmail(bytes)
    }
}