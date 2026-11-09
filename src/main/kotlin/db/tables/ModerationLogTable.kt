package org.ouraircraft.db

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.timestamp

object ModerationLogTable : Table("MODERATION_LOG") {
    val id = long("id").autoIncrement()
    val moderatorId = reference("moderator_id", UserTable.id).index()
    val entityKind = enumerationByName<EntityKind>("entity_kind", 8)
    // Polymorphic identity and deleted proposal IDs cannot have ordinary foreign keys.
    val entityId = long("entity_id")
    val proposalId = long("proposal_id")
    val decision = enumerationByName<ModerationDecision>("decision", 8)
    val comment = text("comment").default("")
    // Required by db_design.md section 7: preserve the submitter's comment on rejection.
    val proposalComment = text("proposal_comment").default("")
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)

    init {
        index(false, entityKind, entityId)
    }
}
