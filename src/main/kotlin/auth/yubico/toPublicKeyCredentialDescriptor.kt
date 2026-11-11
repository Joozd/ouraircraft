package org.ouraircraft.auth.yubico

import com.yubico.webauthn.data.PublicKeyCredentialDescriptor
import org.ouraircraft.model.auth.valueclasses.PasskeyCredentialId

internal fun PasskeyCredentialId.toPublicKeyCredentialDescriptor(): PublicKeyCredentialDescriptor =
    PublicKeyCredentialDescriptor.builder()
        .id(this.toYubicoByteArray())
        .build()