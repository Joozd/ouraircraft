package org.ouraircraft.model.services

import kotlin.uuid.Uuid

interface AuthService {
    /**
     * Make a Json String to pass to the frontend to
     */
    fun makePasskeyRegistrationOptionsJsonString(userId: Uuid)
}