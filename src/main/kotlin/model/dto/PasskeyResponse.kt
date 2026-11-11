package org.ouraircraft.model.dto

import kotlinx.serialization.Serializable
import kotlin.uuid.Uuid

/**
 * Passkey Response gegevens.
 * @param id: De id die deze login-poging definieert
 * @param responseJson De gegevens die nodig zijn om in te loggen. Wat mij betreft opaque;
 *  communicatie tussen Passkey libraries in back- en frontend.
 */
@Serializable
data class PasskeyResponse(val id: Uuid, val responseJson: String)

