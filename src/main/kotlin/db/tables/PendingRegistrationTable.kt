package org.ouraircraft.db.tables

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

/** Pending registrations awaiting email verification. */
object PendingRegistrationTable : Table("PENDING_REGISTRATION") {
    val id = uuid("id")
    val screenName = varchar("screen_name", 64, collate = "NOCASE").uniqueIndex()
    val emailLookup = binary("email_lookup", 32).uniqueIndex()
    val emailEncrypted = binary("email_encrypted")
    val verificationTokenHash = binary("verification_token_hash", 32).uniqueIndex()
    val createdAt = timestamp("created_at")
    val expiresAt = timestamp("expires_at").index()

    override val primaryKey = PrimaryKey(id)
}