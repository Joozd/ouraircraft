package org.ouraircraft.db.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp

/** Stores only the SHA-256 hash; token generation and consumption belong to the application. */
object PasskeyAddingSessionTable : LongIdTable("PASSKEY_SESSION") {
    val userId = reference("user_id", UserTable.id).uniqueIndex()
    val tokenHash = binary("token_hash", 32).uniqueIndex()
    val createdAt = timestamp("created_at")
    val expiresAt = timestamp("expires_at")
}