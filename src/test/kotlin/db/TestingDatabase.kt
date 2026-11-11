package org.ouraircraft.db

import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.ouraircraft.db.tables.*
import java.sql.Connection
import java.sql.DriverManager
import kotlin.uuid.Uuid
import org.jetbrains.exposed.v1.jdbc.Database as ExposedDatabase

/** Owns an isolated SQLite database which survives across repository transactions. */
internal class TestingDatabase : AutoCloseable {
    // A keeper connection preserves the shared in-memory database between transactions.
    private val url = "jdbc:sqlite:file:user-repository-${Uuid.random()}?mode=memory&cache=shared"
    private val keeper: Connection
    val database: Database

    init {
        Class.forName("org.sqlite.JDBC")
        keeper = DriverManager.getConnection(url)
        try {
            database = Database(
                ExposedDatabase.connect(
                    url = url,
                    driver = "org.sqlite.JDBC",
                    databaseConfig = DatabaseConfig {
                        defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
                    },
                ),
            )
            transaction(database.exposed) {
                SchemaUtils.create(
                    UserTable,
                    PendingRegistrationTable,
                    PasskeyTable,
                    AccountRecoveryTokenTable,

                    PasskeyAddingSessionTable,
                    UserSessionTable,

                    AircraftTable,
                    AircraftTypeTable,
                    ManufacturerTable,

                    AircraftTypeProposalTable,
                    AircraftProposalTable,
                    ManufacturerProposalTable,
                )
            }
        } catch (failure: Throwable) {
            keeper.close()
            throw failure
        }
    }

    override fun close() {
        keeper.close()
    }
}
