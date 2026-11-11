package org.ouraircraft.db.tables

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import org.ouraircraft.model.data.CheckResult

/**
 * A [LongIdTable] meant to hold items that can be checked.
 * This is for use in audit trails.
 */
abstract class CheckableTable(name: String = "", columnName: String = "id", sequenceName: String? = null): LongIdTable(name, columnName, sequenceName) {
    val checkedBy = reference("checked_by", UserTable.id).nullable()
    val checkResult = enumerationByName<CheckResult>("check_result", 16).nullable()
    val checkResultComment = text("check_result_comment").nullable()
    val checkedAt = timestamp("checked_at").nullable()
}