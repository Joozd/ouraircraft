package org.ouraircraft.auth

import kotlin.uuid.Uuid

/**
 * The ID of the root user. If no root user found, this is null. there can be only one.
 */
interface RootUser {
    val rootId: Uuid?
}

/**
 * This allows root te be set or changed while running
 */
internal class DevTestingRootUser(): RootUser{
    private var _rootId: Uuid? = null
    override val rootId: Uuid? get() = _rootId

    fun set(id: Uuid){
        _rootId = id
    }
}