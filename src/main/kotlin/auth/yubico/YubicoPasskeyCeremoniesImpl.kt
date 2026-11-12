package org.ouraircraft.auth.yubico

import com.yubico.webauthn.AssertionRequest
import com.yubico.webauthn.AssertionResult
import com.yubico.webauthn.FinishAssertionOptions
import com.yubico.webauthn.FinishRegistrationOptions
import com.yubico.webauthn.RegistrationResult
import com.yubico.webauthn.RelyingParty
import com.yubico.webauthn.StartAssertionOptions
import com.yubico.webauthn.StartRegistrationOptions
import com.yubico.webauthn.data.AuthenticatorSelectionCriteria
import com.yubico.webauthn.data.PublicKeyCredential
import com.yubico.webauthn.data.PublicKeyCredentialCreationOptions
import com.yubico.webauthn.data.ResidentKeyRequirement
import com.yubico.webauthn.exception.AssertionFailedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.ouraircraft.auth.exceptions.BadCredentialsException
import org.ouraircraft.auth.exceptions.RegistrationFailedException
import org.ouraircraft.auth.util.buildCache
import org.ouraircraft.db.exceptions.UserNotFoundByIDException
import org.ouraircraft.model.auth.PasskeyData
import org.ouraircraft.model.dto.PasskeyRegistrationResponse
import org.ouraircraft.model.dto.PasskeyRequest
import org.ouraircraft.model.dto.PasskeyResponse
import org.ouraircraft.model.services.users.PasskeyCeremonies
import org.ouraircraft.model.services.users.PasskeyService
import org.ouraircraft.model.services.users.UserManagementService
import org.ouraircraft.model.auth.valueclasses.PasskeyCredentialId
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.Uuid


class YubicoPasskeyCeremoniesImpl(
    private val relyingParty: RelyingParty,
    private val userManagementService: UserManagementService,
    private val passkeyService: PasskeyService
): PasskeyCeremonies {
    /**
     * Make a JSON String to pass to the frontend, so it can generate a passkey,
     * and send a response to the appropriate API Route.
     */
    private val credentialCreationOptionsMap = buildCache<Uuid, PublicKeyCredentialCreationOptions>(timeOut = 30.minutes)
    private val loginOptionsMap = buildCache<Uuid, AssertionRequest>(timeOut = 30.minutes)

    override suspend fun makePasskeyCreationOptionsJsonString(
        userId: Uuid
    ): String{
        val user = userManagementService.getUserById(userId)
            .getOrThrow() // TODO handle error

        val request: PublicKeyCredentialCreationOptions = relyingParty.startRegistration(
            StartRegistrationOptions.builder()
                .user(user.toUserIdentity())
                .authenticatorSelection(
                    AuthenticatorSelectionCriteria.builder()
                        .residentKey(ResidentKeyRequirement.REQUIRED)
                        .build()
                )
                .build()
        )

        credentialCreationOptionsMap[userId] = request

        return request.toCredentialsCreateJson()
    }

    /**
     * Handle the response to the data created by [makePasskeyCreationOptionsJsonString]
     * @throws RegistrationFailedException if handling the registration response has failed.
     */
    override suspend fun handlePasskeyRegistrationResponse(userId: Uuid, response: PasskeyRegistrationResponse): PasskeyData {
        val publicKeyCredential = PublicKeyCredential.parseRegistrationResponseJson(response.passkeyRegistrationResponseJson)
            .also { println(it)}
        val creationOptions = credentialCreationOptionsMap[userId] ?: throw UserNotFoundByIDException(userId)

        val regResult = try{
            val finishRegistrationOptions = FinishRegistrationOptions.builder()
                .request(creationOptions)
                .response(publicKeyCredential)
                .build()

             relyingParty.finishRegistration(finishRegistrationOptions)
        }
        catch(e: Throwable) {
            println("handlePasskeyRegistrationResponse throws: $e")
            throw (RegistrationFailedException("Handling passkey registration response failed", e))
        }

        val passkeyData = storeCredentialsForUser(creationOptions, regResult, response.label)

        //clean up
        credentialCreationOptionsMap.remove(userId)

        return passkeyData
    }

    /**
     * Make a Json String to pass to the frontend, so it can send a response to the appropriate API Route.
     */
    override suspend fun generateAuthnRequestJson(): PasskeyRequest {
        val options = StartAssertionOptions.builder().build()
        val request = relyingParty.startAssertion(options)
        val loginId = Uuid.generateV4()

        loginOptionsMap[loginId] = request

        val passkeyRequest = PasskeyRequest(loginId, request.toCredentialsGetJson())
        return passkeyRequest
    }

    /**
     * handle the response for a response given to a login attempt
     * @throws org.ouraircraft.auth.exceptions.PasskeyCeremonyNotFoundException when there is no information on how the request started.
     * @throws org.ouraircraft.auth.exceptions.BadCredentialsException if login failed because the provided credentials are not accepted.
     * @return the [Uuid] of the user that provided correct login details.
     */
    override suspend fun handleAuthnRequestResponse(response: PasskeyResponse): PasskeyData {
        val publicKeyCredential = PublicKeyCredential.parseAssertionResponseJson(response.responseJson)
        val request = loginOptionsMap[response.id].also{
            println("Request: $it")
        }



        val result = try{
            val finishAssertionOptions = FinishAssertionOptions.builder()
                .request(request)  // The PublicKeyCredentialRequestOptions from startAssertion above
                .response(publicKeyCredential)
                .build()

            relyingParty.finishAssertion(finishAssertionOptions)
        } catch (e: AssertionFailedException){
            e.printStackTrace()
            throw BadCredentialsException("Login failed due to a problem with the provided Passkey response", e)
        }
        val newPasskeyData = updateCredentialsFromResult(result)

        return newPasskeyData
    }


    private suspend fun storeCredentialsForUser(
        creationOptions: PublicKeyCredentialCreationOptions,
        regResult: RegistrationResult, label: String
    ): PasskeyData{
        val passkeyData = regResult.toPasskeyData(creationOptions, label)
        withContext(Dispatchers.IO) { passkeyService.storePasskeyData(passkeyData) }
        return passkeyData
    }

    /**
     * Returns the newly updated PasskeyData
     */
    private suspend fun updateCredentialsFromResult(assertionResult: AssertionResult): PasskeyData {
        val userId = assertionResult.credential.userHandle.toUuid()
        val credentialID = PasskeyCredentialId(assertionResult.credential.credentialId.bytes)
        val currentPasskeyData = passkeyService.getPasskeyData(credentialID).getOrThrow()!! // TODO handle errors perhaps better
        require(currentPasskeyData.userId == userId) { "Passkey user ID mismatch between DB and response"}

        @Suppress("DEPRECATION") // isBackedUp is experimental, not deprecated
        val newPasskeyData = currentPasskeyData.copy(
            signCount = assertionResult.signatureCount,
            backupState = assertionResult.isBackedUp,
            lastUsedAt = Clock.System.now()
        )

        withContext(Dispatchers.IO) { passkeyService.updatePasskeyData(newPasskeyData) }

        return newPasskeyData
    }
}