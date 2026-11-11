package org.ouraircraft.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class NewUserRegistrationData(val screenName: String, val emailAddress: String)