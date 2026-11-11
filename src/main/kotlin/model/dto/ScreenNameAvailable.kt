package org.ouraircraft.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class ScreenNameAvailable(val screenName: String, val isAvailable: Boolean)
