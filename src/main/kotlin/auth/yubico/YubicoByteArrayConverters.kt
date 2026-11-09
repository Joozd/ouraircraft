package org.ouraircraft.auth.yubico

import org.ouraircraft.model.valueclasses.PasskeyCredentialId
import com.yubico.webauthn.data.ByteArray as YubicoBytearray
import org.ouraircraft.model.valueclasses.PublicKeyCOSE


fun PublicKeyCOSE.toYubicoByteArray(): YubicoBytearray =
    YubicoBytearray(bytes.toByteArray())


fun PasskeyCredentialId.toYubicoByteArray(): YubicoBytearray =
    YubicoBytearray(bytes.toByteArray())