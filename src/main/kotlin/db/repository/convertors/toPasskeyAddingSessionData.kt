package org.ouraircraft.repository.convertors

import org.jetbrains.exposed.v1.core.ResultRow
import org.ouraircraft.db.PasskeyAddingSessionTable
import org.ouraircraft.model.dto.sessions.PasskeyAddingSessionData

/**
 * This needs to be a result from [PasskeyAddingSessionTable]
 */
internal fun ResultRow.toPasskeyAddingSessionData() = PasskeyAddingSessionData(
    userId = this[PasskeyAddingSessionTable.userId],
    createdAt = this[PasskeyAddingSessionTable.createdAt],
    expiresAt = this[PasskeyAddingSessionTable.expiresAt]
)