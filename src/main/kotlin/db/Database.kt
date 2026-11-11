package org.ouraircraft.db

import org.jetbrains.exposed.v1.core.DatabaseConfig
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.ouraircraft.db.tables.*
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.sql.Connection
import org.jetbrains.exposed.v1.jdbc.Database as ExposedDatabase

/**
 * File-backed SQLite database using the Exposed 1.x JDBC DSL.
 * Timestamp columns use java.time.Instant; cryptography and UUIDv7 generation are external.
 *
 * API documentation: https://www.jetbrains.com/help/exposed/working-with-database.html
 * SQLite configuration: https://www.sqlite.org/pragma.html
 */
class Database (val exposed: ExposedDatabase) {
    /** Creates the initial schema and append-only guards; this is not a migration runner. */
    fun createSchema() {
        transaction(exposed) {
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

            // Protect proposal contents and allow review fields to be populated once.
            AircraftProposalTable.createProposalImmutabilityTriggers(AircraftProposalTable.resultAircraftId)
            AircraftTypeProposalTable.createProposalImmutabilityTriggers(AircraftTypeProposalTable.resultAircraftTypeId)
            ManufacturerProposalTable.createProposalImmutabilityTriggers(ManufacturerProposalTable.resultManufacturerId)

        }
    }


    companion object {
        /**
         * Connects to an existing SQLite database or creates a new one.
         *
         * @param location Path to the SQLite database file.
         * @param createNew If true, creates a new database and initializes its schema.
         *                  If false, requires an existing database.
         * @throws FileAlreadyExistsException If createNew is true and the file exists.
         * @throws NoSuchFileException If createNew is false and the file does not exist.
         */
        fun connect(location: Path, createNew: Boolean = false): Database {
            val path = location.toAbsolutePath().normalize()

            if (createNew) {
                Files.createFile(path)
            } else if (Files.notExists(path)) {
                throw NoSuchFileException(path.toString())
            }

            val exposed = ExposedDatabase.connect(
                url = "jdbc:sqlite:$path",
                driver = "org.sqlite.JDBC",
                setupConnection = { connection ->
                    connection.createStatement().use { statement ->
                        statement.execute("PRAGMA foreign_keys = ON")
                        statement.execute("PRAGMA journal_mode = WAL")
                    }
                },
                databaseConfig = DatabaseConfig {
                    defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE
                },
            )

            return Database(exposed).also {
                if (createNew) it.createSchema()
            }
        }
    }

}
