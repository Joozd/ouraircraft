package org.ouraircraft.model.dto

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/**
 * Passkey Request gegevens.
 * @param id: De id die deze login-poging definieert
 * @param requestJson De gegevens die nodig zijn om in te loggen. Wat mij betreft opaque;
 *  communicatie tussen Passkey libraries in back- en frontend.
 */
@Serializable
data class PasskeyRequest(val id: Uuid, val requestJson: String)
