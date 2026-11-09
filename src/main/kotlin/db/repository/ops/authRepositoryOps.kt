package org.ouraircraft.repository.ops

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.update
import org.ouraircraft.db.PasskeyTable
import org.ouraircraft.model.PasskeyData
import org.ouraircraft.model.valueclasses.PasskeyCredentialId
import org.ouraircraft.repository.AuthRepository
import org.ouraircraft.repository.convertors.toPasskeyData
import kotlin.uuid.Uuid

/**
 * Get all Passkey data for user with id [userId]
 * If no passkey data is present, this list is empty.
 */
context(tx: JdbcTransaction)
fun AuthRepository.getPasskeyDataForUserInTransaction(userId: Uuid): List<PasskeyData> {
    val result = PasskeyTable.selectAll()
        .where { PasskeyTable.userId eq userId }
    return result.map { it.toPasskeyData() }
}

/**
 * Get all [PasskeyCredentialId] data for user with id [userId]
 * If no passkey data is present, this list is empty.
 */
context(tx: JdbcTransaction)
fun AuthRepository.getPasskeyCredentialIdsForUserInTransaction(userId: Uuid): List<PasskeyCredentialId> {
    val result = PasskeyTable.select(PasskeyTable.credentialId)
        .where { PasskeyTable.userId eq userId }
    return result.map { PasskeyCredentialId(it[PasskeyTable.credentialId]) }
}

/**
 * Get [PasskeyData] for [credentialId]
 * If no passkey data is present, returns null
 */
context(tx: JdbcTransaction)
fun AuthRepository.getPasskeyDataInTransaction(
    credentialId: PasskeyCredentialId
): PasskeyData? = PasskeyTable.selectAll()
        .where { PasskeyTable.credentialId eq credentialId.bytes.toByteArray() }
        .firstOrNull()
        ?.toPasskeyData()

/**
 * Inserts [passkeyData] into the database within the current transaction.
 */
context(tx: JdbcTransaction)
fun AuthRepository.writePasskeyDataInTransaction(passkeyData: PasskeyData) {
    require(passkeyData.dbId == null) { "only write new passkey data using writePasskeyDataInTransaction, use updatePasskeyDataInTransaction for updates."}
    PasskeyTable.insert {
        it[userId] = passkeyData.userId
        it[credentialId] = passkeyData.credentialId.bytes.toByteArray()
        it[publicKeyCose] = passkeyData.publicKeyCose.bytes.toByteArray()
        it[signCount] = passkeyData.signCount
        it[transports] = Json.encodeToString(passkeyData.transports)
        it[name] = passkeyData.label
        it[backupEligible] = passkeyData.backupEligible
        it[backupState] = passkeyData.backupState
        it[createdAt] = passkeyData.createdAt
        it[lastUsedAt] = passkeyData.lastUsedAt
    }
}

/**
 * Inserts [passkeyData] into the database within the current transaction.
 */
context(tx: JdbcTransaction)
fun AuthRepository.updatePasskeyDataInTransaction(passkeyData: PasskeyData) {
    require(passkeyData.dbId != null) { "write new passkey data using writePasskeyDataInTransaction, use updatePasskeyDataInTransaction for updates."}
    PasskeyTable.update({ PasskeyTable.id eq passkeyData.dbId }) {
        it[userId] = passkeyData.userId
        it[credentialId] = passkeyData.credentialId.bytes.toByteArray()
        it[publicKeyCose] = passkeyData.publicKeyCose.bytes.toByteArray()
        it[signCount] = passkeyData.signCount
        it[transports] = Json.encodeToString(passkeyData.transports)
        it[name] = passkeyData.label
        it[backupEligible] = passkeyData.backupEligible
        it[backupState] = passkeyData.backupState
        it[createdAt] = passkeyData.createdAt
        it[lastUsedAt] = passkeyData.lastUsedAt
    }
}





