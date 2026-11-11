@file:Suppress("UNUSED_PARAMETER")

package org.ouraircraft.db.repository.ops

import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.statements.UpdateStatement
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.update
import org.ouraircraft.db.tables.CheckableTable
import org.ouraircraft.model.auth.User
import org.ouraircraft.model.data.CheckResult
import kotlin.time.Clock

/**
 * Perform the update in [block]
 * on the rows found by [where]
 * This also fill the common fields of a [CheckableTable].
 *
 */
context(tx: JdbcTransaction)
internal fun <T: CheckableTable> T.updateFromCheck(
    where: () -> Op<Boolean>,
    user: User,
    checkResult: CheckResult,
    comment: String?,
    block: T.(UpdateStatement) -> Unit
): Int = this.update(where){ row ->
    block(row)

    row[this.checkedBy] = user.id
    row[this.checkResult] = checkResult
    row[this.checkResultComment] = comment
    row[this.checkedAt] = Clock.System.now()
}