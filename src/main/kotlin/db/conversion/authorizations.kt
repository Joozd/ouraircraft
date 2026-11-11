package org.ouraircraft.db.conversion

import org.ouraircraft.auth.UserAuthorization
import org.ouraircraft.db.UserRole


// Add authorization sets for each role
private val standard: Set<UserAuthorization> = setOf()
private val superUser: Set<UserAuthorization> = setOf()
// private val root: Set<UserAuthorization> = setOf()

fun UserRole.toAuthorizations(): Set<UserAuthorization> {
    return when (this) {
        UserRole.USER -> standard
        UserRole.SUPERUSER -> superUser
    }
}