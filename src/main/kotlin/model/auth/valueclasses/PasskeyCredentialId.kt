package org.ouraircraft.model.auth.valueclasses

import kotlinx.io.bytestring.ByteString
import org.ouraircraft.auth.util.toByteString


/**
 * a PasskeyCredentialId identifies a passkey credential
 */
@JvmInline
value class PasskeyCredentialId(val bytes: ByteString){
    constructor(bytes: ByteArray): this(bytes.toByteString())
}