package org.ouraircraft.db.exceptions

/**
 * This is thrown when a Proposal is being checked that is already marked as checked.
 */
class AlreadyCheckedException(reason: String): IllegalStateException(reason)