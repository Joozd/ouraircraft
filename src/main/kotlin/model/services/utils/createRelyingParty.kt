package org.ouraircraft.model.services.utils

import com.yubico.webauthn.RelyingParty
import com.yubico.webauthn.data.RelyingPartyIdentity
import org.ouraircraft.auth.yubico.CredentialRepositoryImpl
import org.ouraircraft.model.internal.Configuration
import org.ouraircraft.model.services.users.PasskeyService

internal fun createRelyingParty(config: Configuration, passkeyService: PasskeyService): RelyingParty{
    val rpIdentity = RelyingPartyIdentity.builder()
        .id(config.hostName!!)  // Set this to a parent domain that covers all subdomains
        // where users' credentials should be valid
        .name(config.programName)
        .build()

    return RelyingParty.builder()
        .identity(rpIdentity)
        .credentialRepository(CredentialRepositoryImpl(passkeyService))
        .origins(if(config.hostName == "localhost") setOf("http://localhost") else setOf("https://${config.hostName}"))
        .build()
}