package org.ouraircraft.db

import java.nio.file.Path
import java.sql.Connection
import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.Database as ExposedDatabase
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

/**
 * File-backed SQLite database using the Exposed 1.x JDBC DSL.
 * Timestamp columns use java.time.Instant; cryptography and UUIDv7 generation are external.
 *
 * API documentation: https://www.jetbrains.com/help/exposed/working-with-database.html
 * SQLite configuration: https://www.sqlite.org/pragma.html
 */
class Database private constructor(val exposed: ExposedDatabase) {
    /** Creates the initial schema and append-only guards; this is not a migration runner. */
    fun createSchema() {
        transaction(exposed) {
            SchemaUtils.create(
                UserTable,
                AircraftTable,
                AircraftTypeTable,
                PasskeyTable,
                AccountRecoveryTokenTable,
                TypeRevisionTable,
                AircraftRevisionTable,
                PendingTypeRevisionTable,
                PendingAircraftRevisionTable,
                ModerationLogTable,
            )

            for (tableName in listOf("TYPE_REVISION", "AIRCRAFT_REVISION")) {
                for (operation in listOf("UPDATE", "DELETE")) {
                    exec(
                        """
                        CREATE TRIGGER IF NOT EXISTS "${tableName}_NO_${operation}"
                        BEFORE $operation ON "$tableName"
                        BEGIN
                            SELECT RAISE(ABORT, 'Accepted revisions are append-only');
                        END
                        """.trimIndent(),
                    )
                }
            }
        }
    }

    companion object {
        /** The database file's parent directory must already exist. */
        fun connect(location: Path): Database {
            val exposed = ExposedDatabase.connect(
                url = "jdbc:sqlite:${location.toAbsolutePath().normalize()}",
                driver = "org.sqlite.JDBC",
                setupConnection = { connection ->
                    // Configure before Exposed starts a transaction, on every connection.
                    connection.createStatement().use { statement ->
                        statement.execute("PRAGMA foreign_keys = ON")
                        statement.execute("PRAGMA journal_mode = WAL")
                    }
                },
                databaseConfig = DatabaseConfig {
                    defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
                },
            )
            return Database(exposed)
        }
    }
}
