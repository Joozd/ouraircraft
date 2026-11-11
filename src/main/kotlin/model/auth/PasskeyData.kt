package org.ouraircraft.model.auth

import org.ouraircraft.model.auth.valueclasses.PasskeyCredentialId
import org.ouraircraft.model.auth.valueclasses.PublicKeyCOSE
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * A registered passkey and its stored authentication metadata.
 */
data class PasskeyData(
    val dbId: Long? = null,
    val userId: Uuid,
    val credentialId: PasskeyCredentialId,
    val publicKeyCose: PublicKeyCOSE,
    val signCount: Long,
    val transports: Set<String>,
    val label: String,
    val backupEligible: Boolean,
    val backupState: Boolean,
    val createdAt: Instant,
    val lastUsedAt: Instant,
)