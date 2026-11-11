package org.ouraircraft.db.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.core.isNull
import org.jetbrains.exposed.v1.datetime.timestamp
import org.ouraircraft.model.data.CheckResult

/**
 * Base table for records that require review, primarily used for audit trails.
 *
 * Provides common fields for creation timestamps, review decisions,
 * reviewers, and optional review comments.
 *
 * Pending records have a null [checkResult]. Review fields are populated
 * once when a record is accepted or rejected.
 *
 * Defines a partial index on [createdAt] containing only pending records
 * ([checkResult] IS NULL). This enables efficient retrieval of pending
 * records ordered by creation time, without indexing reviewed records.
 *
 * @param name Database table name.
 * @param columnName Name of the primary key column.
 * @param sequenceName Optional database sequence name.
 */
abstract class CheckableTable(name: String = "", columnName: String = "id", sequenceName: String? = null): LongIdTable(name, columnName, sequenceName) {
    val checkedBy = reference("checked_by", UserTable.id).nullable()
    val checkResult = enumerationByName<CheckResult>("check_result", 16).nullable()
    val checkResultComment = text("check_result_comment").nullable()
    val createdAt = timestamp("created_at")
    val checkedAt = timestamp("checked_at").nullable()

    init {
        index(
            customIndexName = "${name}_pending_created_at",
            columns = arrayOf(createdAt),
            filterCondition = { checkResult.isNull() }
        )
    }
}