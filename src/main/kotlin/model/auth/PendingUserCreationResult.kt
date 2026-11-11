package org.ouraircraft.model.auth

import org.ouraircraft.auth.EmailVerificationToken
import kotlin.uuid.Uuid

data class PendingUserCreationResult(val id: Uuid, val emailVerificationToken: EmailVerificationToken)