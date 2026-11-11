package org.ouraircraft.auth.yubico

import com.yubico.webauthn.RegisteredCredential
import org.ouraircraft.model.auth.PasskeyData
import com.yubico.webauthn.data.ByteArray as YubicoBytearray
import java.util.Optional

/**
 * Converts stored passkey data to a Yubico credential, or an empty optional.
 */
internal fun PasskeyData?.toRegisteredCredentialOptional(): Optional<RegisteredCredential> =
    Optional.ofNullable(this.toRegisteredCredential())


internal fun PasskeyData?.toRegisteredCredential() =
    this?.let { data ->
        @Suppress("DEPRECATION") // Experimental `backupEligible` and `backupState`
        RegisteredCredential.builder()
            .credentialId(data.credentialId.toYubicoByteArray())
            .userHandle(YubicoBytearray(data.userId.toByteArray()))
            .publicKeyCose(data.publicKeyCose.toYubicoByteArray())
            .signatureCount(data.signCount)
            .backupEligible(data.backupEligible)
            .backupState(data.backupState)
            .build()
    }