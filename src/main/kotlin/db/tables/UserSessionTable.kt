package org.ouraircraft.db

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.Instant

/** Stores only the SHA-256 hash; token generation and consumption belong to the application. */
object UserSessionTable : LongIdTable("USER_SESSION") {
    val userId = reference("user_id", UserTable.id).index()
    val tokenHash = binary("token_hash", 32).uniqueIndex()
    val createdAt = timestamp("created_at")
    val expiresAt = timestamp("expires_at")
    val maxExtendedExpiresAt = timestamp("max_expires_at")
}