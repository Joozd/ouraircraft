package org.ouraircraft.crypt

import kotlinx.io.bytestring.ByteString
import org.ouraircraft.auth.util.toByteString


/** A 32-byte HMAC-SHA256 digest of a recovery token. */
@JvmInline
value class HashedToken(val bytes: ByteString) {
    constructor(bytes: ByteArray) : this(bytes.toByteString())
    init {
        require(bytes.size == 32)
    }
}
