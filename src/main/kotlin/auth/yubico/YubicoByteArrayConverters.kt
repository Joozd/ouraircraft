package org.ouraircraft.auth.yubico

import org.ouraircraft.model.auth.valueclasses.PasskeyCredentialId
import com.yubico.webauthn.data.ByteArray as YubicoBytearray
import org.ouraircraft.model.auth.valueclasses.PublicKeyCOSE
import kotlin.uuid.Uuid


fun PublicKeyCOSE.toYubicoByteArray(): YubicoBytearray =
    YubicoBytearray(bytes.toByteArray())


fun PasskeyCredentialId.toYubicoByteArray(): YubicoBytearray =
    YubicoBytearray(bytes.toByteArray())

fun Uuid.toYubicoByteArray(): YubicoBytearray =
    YubicoBytearray(this.toByteArray())

fun YubicoBytearray.toUuid(): Uuid =
    Uuid.fromByteArray(this.bytes)