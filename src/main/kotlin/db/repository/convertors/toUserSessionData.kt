package org.ouraircraft.repository.convertors

import org.jetbrains.exposed.v1.core.ResultRow
import org.ouraircraft.db.UserSessionTable
import org.ouraircraft.model.dto.sessions.UserSessionData

/**
 * This needs to be a result from [UserSessionTable]
 */
internal fun ResultRow.toUserSessionData() = UserSessionData(
    sessionId = this[UserSessionTable.id].value,
    userId = this[UserSessionTable.userId],
    createdAt = this[UserSessionTable.createdAt],
    expiresAt = this[UserSessionTable.expiresAt],
    maxExtendedExpiresAt = this[UserSessionTable.maxExtendedExpiresAt]
)