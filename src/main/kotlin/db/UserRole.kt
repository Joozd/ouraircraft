package org.ouraircraft.db

/**
 * ROOT is elevated from another source. DB only knows USER and SUPERUSER.
 */
enum class UserRole {
    USER,
    SUPERUSER,
}
