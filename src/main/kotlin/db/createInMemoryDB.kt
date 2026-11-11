package org.ouraircraft.db

import org.jetbrains.exposed.v1.core.DatabaseConfig
import java.sql.Connection
import java.sql.DriverManager
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.jdbc.Database as ExposedDatabase

private var keepAlive: Connection? = null

/**
 * Creates a fresh, in-memory SQLite database with the application schema.
 *
 * Uses a unique database URL to isolate each instance and enables shared cache
 * so multiple JDBC connections can access the same database.
 *
 * A keep-alive connection remains open to prevent SQLite from discarding the
 * database between transactions. This connection is intentionally not closed,
 * as this function is intended for local development and testing only.
 *
 * The database and its contents are discarded when the JVM terminates. Sloppy, but ok for dev.
 *
 ************************************
 * THIS IS NOT MEANT FOR PRODUCTION *
 ************************************
 *
 * @return A [Database] initialized with the application schema.
 */
internal fun createInMemoryDB(): Database {
    val url = "jdbc:sqlite:file:user-repository-${Uuid.random()}?mode=memory&cache=shared"
    keepAlive = DriverManager.getConnection(url)

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

