package org.ouraircraft.crypt

import kotlinx.io.bytestring.ByteString
import org.ouraircraft.auth.util.toByteString


/** A 32-byte HMAC-SHA256 digest of an email address. */
@JvmInline
value class HashedEmail(val bytes: ByteString) {
    constructor(bytes: ByteArray) : this(bytes.toByteString())
    init {
        require(bytes.size == 32)
    }
}
