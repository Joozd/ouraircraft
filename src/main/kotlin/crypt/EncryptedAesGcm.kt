package org.ouraircraft.crypt

import kotlinx.io.bytestring.ByteString
import org.ouraircraft.auth.util.toByteString


/** AES-GCM encrypted data, including nonce and authentication tag. */
@JvmInline
value class EncryptedAesGcm(val bytes: ByteString)

/** an EncryptedAesGcm that is expected to contain an encrypted UTF-8 encoded string (not enforced here) */
@JvmInline
value class EncryptedAesGcmText(val bytes: ByteString){
    constructor(bytes: ByteArray) : this(bytes.toByteString())
}
