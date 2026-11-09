package org.ouraircraft.crypt

import kotlinx.io.bytestring.ByteString


/** A 32-byte HMAC-SHA256 digest of a recovery token. */
@JvmInline
value class HashedRecoveryToken(val bytes: ByteString) {
    init {
        require(bytes.size == 32)
    }
}
