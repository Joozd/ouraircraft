package org.ouraircraft.auth.yubico

import com.yubico.webauthn.RegistrationResult
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions
import org.ouraircraft.model.auth.PasskeyData
import org.ouraircraft.model.auth.valueclasses.PasskeyCredentialId
import org.ouraircraft.model.auth.valueclasses.PublicKeyCOSE
import kotlin.collections.emptySet
import kotlin.jvm.optionals.getOrNull
import kotlin.time.Clock

internal fun RegistrationResult.toPasskeyData(creationOptions: PublicKeyCredentialCreationOptions, label: String
): PasskeyData =
    @Suppress("DEPRECATION")
    PasskeyData(
        userId = creationOptions.user.id.toUuid(),
        credentialId = PasskeyCredentialId(keyId.id.bytes),
        publicKeyCose = PublicKeyCOSE(publicKeyCose.bytes),
        signCount = signatureCount,
        transports = keyId.transports.getOrNull()?.map { it.id }?.toSet() ?: emptySet(),
        label = label,
        backupEligible = isBackupEligible,
        backupState = isBackedUp,
        createdAt = Clock.System.now(),
        lastUsedAt = Clock.System.now(),
    )
