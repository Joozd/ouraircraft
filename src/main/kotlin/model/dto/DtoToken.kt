package org.ouraircraft.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class DtoToken(val base64UrlSafe: String)