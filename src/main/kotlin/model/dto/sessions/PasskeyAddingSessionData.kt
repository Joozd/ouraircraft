package org.ouraircraft.model.dto.sessions

import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Holds the data for a PasskeyAddingSession.
 * This does not include the token, as that is only known upon creation.
 * @property sessionId The internal ID of the session. Null if created new, the database ID if retrieved from DB.
 */
data class PasskeyAddingSessionData(
    val sessionId: Long,
    val userId: Uuid,
    val createdAt: Instant,
    val expiresAt: Instant
)