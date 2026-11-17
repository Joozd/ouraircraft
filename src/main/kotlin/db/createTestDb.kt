package org.ouraircraft.db

import org.jetbrains.exposed.v1.core.DatabaseConfig
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import org.jetbrains.exposed.v1.jdbc.Database as ExposedDatabase

/**
 * Opens a file-backed SQLite database for development and testing.
 *
 * Creates the parent directory and database file if needed.
 * Existing data is retained; the database is not automatically deleted.
 */
internal fun createFileTestDB(): Database {
    val path = Path.of("C:/temp/ouraircraft.db")
    Files.createDirectories(path.parent)

    val url = "jdbc:sqlite:$path?journal_mode=WAL&busy_timeout=5000"

    return Database(
        ExposedDatabase.connect(
            url = url,
            driver = "org.sqlite.JDBC",
            databaseConfig = DatabaseConfig {
                defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
            },
        ),
    ).apply {
        createSchema()
    }
}