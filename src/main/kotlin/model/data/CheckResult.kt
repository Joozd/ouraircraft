package org.ouraircraft.model.data

enum class CheckResult {
    APPROVED,
    REJECTED
}

/**
 * This helper function guarantees that there are ony two states: ACCEPTED and REJECTED
 */
fun CheckResult.isApproved(): Boolean =
    this == CheckResult.APPROVED
