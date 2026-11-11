package org.ouraircraft.db.repository.convertors

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.ResultRow
import org.ouraircraft.db.tables.PasskeyTable
import org.ouraircraft.model.auth.PasskeyData
import org.ouraircraft.model.auth.valueclasses.PasskeyCredentialId
import org.ouraircraft.model.auth.valueclasses.PublicKeyCOSE

/**
 * Creates passkey data from a [ResultRow] containing [PasskeyTable] columns.
 */
fun ResultRow.toPasskeyData(): PasskeyData = PasskeyData(
    dbId = this[PasskeyTable.id].value,
    userId = this[PasskeyTable.userId],
    credentialId = PasskeyCredentialId(this[PasskeyTable.credentialId]),
    publicKeyCose = PublicKeyCOSE(this[PasskeyTable.publicKeyCose]),
    signCount = this[PasskeyTable.signCount],
    transports = this[PasskeyTable.transports].let {
        Json.decodeFromString<Set<String>>(it)
    },
    label = this[PasskeyTable.name],
    backupEligible = this[PasskeyTable.backupEligible],
    backupState = this[PasskeyTable.backupState],
    createdAt = this[PasskeyTable.createdAt],
    lastUsedAt = this[PasskeyTable.lastUsedAt],
)