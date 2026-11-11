package org.ouraircraft.db.utils

import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder

internal fun op(block: QueryBuilder.() -> Unit) = object : Op<Boolean>() {
    override fun toQueryBuilder(queryBuilder: QueryBuilder) {
        queryBuilder.block()
    }
}