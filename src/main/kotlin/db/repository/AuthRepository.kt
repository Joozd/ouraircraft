package org.ouraircraft.repository

import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.ouraircraft.db.Database
import org.ouraircraft.model.PasskeyData
import org.ouraircraft.model.valueclasses.PasskeyCredentialId
import org.ouraircraft.repository.ops.getPasskeyCredentialIdsForUserInTransaction
import org.ouraircraft.repository.ops.getPasskeyDataInTransaction
import org.ouraircraft.repository.ops.updatePasskeyDataInTransaction
import org.ouraircraft.repository.ops.writePasskeyDataInTransaction
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