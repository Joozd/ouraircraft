package org.ouraircraft.auth.yubico

import com.yubico.webauthn.RelyingParty
import com.yubico.webauthn.StartRegistrationOptions
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions
import com.yubico.webauthn.data.UserIdentity
import org.ouraircraft.model.services.AuthService
import org.ouraircraft.model.services.UserManagementService
import org.ouraircraft.repository.UserRepository
import kotlin.uuid.Uuid


class YubicoAuthServiceImpl(
    private val relyingParty: RelyingParty,
    private val userManagementService: UserManagementService
): AuthService {
    private val credentialCreationOptionsMap = HashMap<Uuid, PublicKeyCredentialCreationOptions>()

    suspend fun makePasskeyRegistrationOptionsJsonString(
        userId: Uuid
    ): String{
        val user = userManagementService.getUserById(userId)
            .getOrThrow() // TODO handle error

        val request: PublicKeyCredentialCreationOptions = relyingParty.startRegistration(
            StartRegistrationOptions.builder()
                .user(user.toUserIdentity())
                .build()
        )

        return request.toCredentialsCreateJson()
    }
}