package org.ouraircraft.db.utils

import org.jetbrains.exposed.v1.core.Expression
import org.jetbrains.exposed.v1.core.QueryBuilder

internal fun QueryBuilder.appendAll(vararg args: Any): QueryBuilder {
    for (arg in args) {
        when (arg) {
            is String -> append(arg)
            is Expression<*> -> append(arg)
            else -> error("Unsupported argument type: ${arg::class}")
        }
    }
    return this
}