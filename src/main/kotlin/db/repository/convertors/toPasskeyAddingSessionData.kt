package org.ouraircraft.db.repository.convertors

import org.jetbrains.exposed.v1.core.ResultRow
import org.ouraircraft.db.tables.PasskeyAddingSessionTable
import org.ouraircraft.model.dto.sessions.PasskeyAddingSessionData

/**
 * This needs to be a result from [PasskeyAddingSessionTable]
 */
internal fun ResultRow.toPasskeyAddingSessionData() = PasskeyAddingSessionData(
    sessionId = this[PasskeyAddingSessionTable.id].value,
    userId = this[PasskeyAddingSessionTable.userId],
    createdAt = this[PasskeyAddingSessionTable.createdAt],
    expiresAt = this[PasskeyAddingSessionTable.expiresAt]
)