package org.ouraircraft.auth.yubico

import com.yubico.webauthn.CredentialRepository
import com.yubico.webauthn.RegisteredCredential
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor
import kotlinx.coroutines.runBlocking
import org.ouraircraft.auth.util.mapToOptional
import org.ouraircraft.auth.util.toOptional
import org.ouraircraft.model.services.users.PasskeyService
import org.ouraircraft.model.auth.valueclasses.PasskeyCredentialId
import java.util.*
import com.yubico.webauthn.data.ByteArray as YubicoBytearray

class CredentialRepositoryImpl(private val passkeyService: PasskeyService): CredentialRepository {
    /**
     * Get the credential IDs of all credentials registered to the user with the given username.
     * After a successful registration ceremony, the RegistrationResult.getKeyId() method returns a value suitable for inclusion in this set.
     */
    override fun getCredentialIdsForUsername(username: String): Set<PublicKeyCredentialDescriptor> =
        passkeyService.getPasskeyCredentialIdsForScreenName(username)
            .getOrThrow() // TODO handle errors
            .map { it.toPublicKeyCredentialDescriptor() }
            .toSet()

    /**
     * Get the user handle corresponding to the given username - the inverse of getUsernameForUserHandle(ByteArray).
     * Used to look up the user handle based on the username, for authentication ceremonies where the username is already given.
     */
    override fun getUserHandleForUsername(username: String): Optional<YubicoBytearray> =
        runBlocking {
            passkeyService.getIdFromScreenName(username).mapToOptional{ it.toYubicoByteArray() }
        }

    /**
     * Get the username corresponding to the given user handle - the inverse of getUserHandleForUsername(String).
     * Used to look up the username based on the user handle, for username-less authentication ceremonies.
     * Implementations of this method MUST NOT return null.
     */
    override fun getUsernameForUserHandle(userHandle: YubicoBytearray): Optional<String> =
        runBlocking {
            passkeyService.getScreenNameFromId(userHandle.toUuid()).toOptional()
        }

    /**
     * Look up the public key and stored signature count for the given credential registered to the given user.
     * The returned RegisteredCredential is not expected to be long-lived. It may be read directly from a database or assembled from other components.
     * Implementations of this method MUST NOT return null.
     */
    override fun lookup(
        credentialId: YubicoBytearray,
        userHandle: YubicoBytearray
    ): Optional<RegisteredCredential> =
        passkeyService.getPasskeyData(userHandle.toUuid(), PasskeyCredentialId(credentialId.bytes))
            .getOrThrow() // TODO handle errors
            .toRegisteredCredentialOptional()

    /**
     * Look up all credentials with the given credential ID, regardless of what user they're registered to.
     * This is used to refuse registration of duplicate credential IDs. Therefore, under normal circumstances this method should only return zero or one credential (this is an expected consequence, not an interface requirement).
     * Implementations of this method MUST NOT return null.
     */
    override fun lookupAll(credentialId: YubicoBytearray): Set<RegisteredCredential> =
        setOfNotNull(
            passkeyService.getPasskeyData(PasskeyCredentialId(credentialId.bytes))
                .getOrThrow() // TODO handle errors
                ?.toRegisteredCredential()
        )
}
