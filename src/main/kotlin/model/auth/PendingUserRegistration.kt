package org.ouraircraft.model.auth

import org.ouraircraft.crypt.EncryptedAesGcmText
import org.ouraircraft.crypt.HashedEmail
import org.ouraircraft.crypt.HashedToken
import kotlin.time.Instant
import kotlin.uuid.Uuid

data class PendingUserRegistration(
    val id: Uuid,
    val screenName: String,
    val emailLookup: HashedEmail,
    val emailEncrypted: EncryptedAesGcmText,
    val verificationTokenHash: HashedToken,
    val createdAt: Instant,
    val expiresAt: Instant
)