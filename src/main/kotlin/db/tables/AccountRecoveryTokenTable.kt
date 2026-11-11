package org.ouraircraft.db.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

/** Stores only the SHA-256 hash; token generation and consumption belong to the application. */
object AccountRecoveryTokenTable : LongIdTable("ACCOUNT_RECOVERY_TOKEN") {
    val userId = reference("user_id", UserTable.id).uniqueIndex()
    val tokenHash = binary("token_hash", 32).uniqueIndex()
    val expiresAt = timestamp("expires_at")
}
