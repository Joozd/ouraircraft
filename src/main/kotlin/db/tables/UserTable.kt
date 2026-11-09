package org.ouraircraft.db

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

/** UUIDv7 IDs and encrypted/hashed email values must be supplied by the application. */
object UserTable : Table("USER") {
    val id = uuid("id")
    val screenName = varchar("screen_name", 64, collate = "NOCASE").uniqueIndex()
    val emailLookup = binary("email_lookup", 32).uniqueIndex()
    val emailEncrypted = binary("email_encrypted")
    val role = enumerationByName<UserRole>("role", 9)
    val disabled = bool("disabled")
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}
