package org.ouraircraft.db

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.ouraircraft.db.tables.CheckableTable

/**
 * Creates SQLite triggers that prevent deletion and protect all proposal columns.
 *
 * Columns in [fillOnceColumns] may change only while their previous value is null.
 * All other columns must remain unchanged. Updates that preserve values are allowed.
 *
 * Must be called after this table has been created.
 * Existing triggers with the generated names are left unchanged.
 */
context(tx: JdbcTransaction)
internal fun Table.createProposalImmutabilityTriggers(
    vararg fillOnceColumns: Column<*>
) {
    require(fillOnceColumns.all { it.table == this }) {
        "All fill-once columns must belong to this table"
    }

    val fillOnce = fillOnceColumns.toSet()

    /** Quotes a SQLite identifier, escaping embedded quotation marks. */
    fun quote(identifier: String): String =
        "\"" + identifier.replace("\"", "\"\"") + "\""

    val updateCondition = columns.joinToString("\nOR ") { column ->
        val name = quote(column.name)
        val changed = "NEW.$name IS NOT OLD.$name"

        if (column in fillOnce) {
            "(OLD.$name IS NOT NULL AND $changed)"
        } else {
            changed
        }
    }

    tx.exec(
        """
        CREATE TRIGGER IF NOT EXISTS ${quote("${tableName}_update_guard")}
        BEFORE UPDATE ON ${quote(tableName)}
        FOR EACH ROW
        WHEN $updateCondition
        BEGIN
            SELECT RAISE(ABORT, 'Proposal fields are immutable once populated');
        END;
        """.trimIndent()
    )

    tx.exec(
        """
        CREATE TRIGGER IF NOT EXISTS ${quote("${tableName}_delete_guard")}
        BEFORE DELETE ON ${quote(tableName)}
        FOR EACH ROW
        BEGIN
            SELECT RAISE(ABORT, 'Proposals cannot be deleted');
        END;
        """.trimIndent()
    )
}


/**
 * Creates immutability triggers, allowing review fields and [extraFillOnceColumns]
 * to be populated once. All other columns are immutable; deletion is prohibited.
 *
 * Must be called after this table has been created.
 */
context(tx: JdbcTransaction)
internal fun CheckableTable.createProposalImmutabilityTriggers(
    vararg extraFillOnceColumns: Column<*>
) {
    //This cast makes it call the generic function and not itself
    (this as Table).createProposalImmutabilityTriggers(
        checkedBy,
        checkResult,
        checkResultComment,
        checkedAt,
        *extraFillOnceColumns
    )
}