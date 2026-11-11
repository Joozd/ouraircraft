package org.ouraircraft.db.repository

import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.ouraircraft.db.Database
import org.ouraircraft.db.repository.ops.*
import org.ouraircraft.model.auth.PasskeyData
import org.ouraircraft.model.auth.valueclasses.PasskeyCredentialId
import kotlin.uuid.Uuid

/**
 * Provides persistence operations for authentication operations.
 * using the Exposed JDBC DSL.
 *
 * Each operation runs in its own database transaction.
 *
 * @param database Database connection used for persistence.
 */
class AuthRepository(private val database: Database) {
    /**
     * Get all Passkey data for user with id [userId]
     * If no passkey data is present, this list is empty.
     */
    fun getPasskeyCredentialIdsForUser(userId: Uuid): List<PasskeyCredentialId> =
        transaction(database.exposed) {
            getPasskeyCredentialIdsForUserInTransaction(userId)
        }

    /**
     * Get all Passkey data for user with screen name [screenName]
     * If no passkey data is present, this list is empty.
     */
    fun getPasskeyCredentialIdsForUser(screenName: String): List<PasskeyCredentialId> =
        transaction(database.exposed) {
            getPasskeyCredentialIdsForScreenNameInTransaction(screenName)
        }

    fun getPasskeyData(
        credentialId: PasskeyCredentialId
    ): PasskeyData? =
        transaction(database.exposed) {
            getPasskeyDataInTransaction(credentialId)
        }

    /**
     * Write passkey data.
     * This needs a [PasskeyData] object with an empty (null) [PasskeyData.dbId]
     */
    fun writePasskeyData(passkeyData: PasskeyData)=
        transaction(database.exposed) {
            writePasskeyDataInTransaction(passkeyData)
        }

    /**
     * Update passkey data.
     * This needs a [PasskeyData] object with an existing [PasskeyData.dbId]
     */
    fun updatePasskeyData(passkeyData: PasskeyData)=
        transaction(database.exposed) {
            updatePasskeyDataInTransaction(passkeyData)
        }


}