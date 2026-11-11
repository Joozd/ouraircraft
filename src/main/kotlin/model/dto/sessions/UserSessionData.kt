package org.ouraircraft.model.dto.sessions

import kotlinx.serialization.Serializable
import kotlin.time.Instant
import kotlin.uuid.Uuid


/**
 * Holds the data for a UserSession.
 * This does not include the token, as that is only known upon creation.
 * @property sessionId The internal ID of this session
 * @property userId The [Uuid] of the user this session belongs to
 * @property createdAt The timestamp when this session was first created
 * @property expiresAt The timestamp from when this session no longer valid
 * @property maxExtendedExpiresAt The maximum allowed time to extend the session duration without needing to re-login.
 */
@Serializable
data class UserSessionData(
    val sessionId: Long,
    val userId: Uuid,
    val createdAt: Instant,
    val expiresAt: Instant,
    val maxExtendedExpiresAt: Instant
){
    init{
        require(maxExtendedExpiresAt >= expiresAt) { "expiresAt($expiresAt) cannot be after maxExtendedExpiresAt($maxExtendedExpiresAt)" }
    }
}