package org.ouraircraft.db.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

object PasskeyTable : LongIdTable("PASSKEY") {
    val userId = reference("user_id", UserTable.id).index()
    val credentialId = binary("credential_id").uniqueIndex()
    val publicKeyCose = binary("public_key_cose")
    val signCount = long("sign_count")
    val transports = text("transports")
    val name = text("name")
    val backupEligible = bool("backup_eligible")
    val backupState = bool("backup_state")
    val createdAt = timestamp("created_at")
    val lastUsedAt = timestamp("last_used_at")
}
