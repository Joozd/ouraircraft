package org.ouraircraft.model.policy

import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

private const val PENDING_REGISTRATION_TIMEOUT_HOURS = 24L

fun pendingRegistrationTimeout(): Instant =
    Clock.System.now() + PENDING_REGISTRATION_TIMEOUT_HOURS.hours
