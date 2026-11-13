package org.ouraircraft.model.services.utils

import com.yubico.webauthn.RelyingParty
import com.yubico.webauthn.data.RelyingPartyIdentity
import org.ouraircraft.CliArgs
import org.ouraircraft.auth.yubico.CredentialRepositoryImpl
import org.ouraircraft.model.services.users.PasskeyService

internal fun createRelyingParty(args: CliArgs, passkeyService: PasskeyService): RelyingParty{
    val rpIdentity = RelyingPartyIdentity.builder()
        .id(args.hostName!!)  // Set this to a parent domain that covers all subdomains
        // where users' credentials should be valid
        .name("OurAircraft") // TODO get this from config
        .build()

    return RelyingParty.builder()
        .identity(rpIdentity)
        .credentialRepository(CredentialRepositoryImpl(passkeyService))
        .origins(if(args.hostName == "localhost") setOf("http://localhost") else setOf("https://${args.hostName}"))
        .build()
}