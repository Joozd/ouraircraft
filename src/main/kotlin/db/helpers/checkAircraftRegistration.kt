package org.ouraircraft.db.helpers

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.Table.Dual.check
import org.jetbrains.exposed.v1.core.Table.Dual.transform
import org.ouraircraft.db.utils.appendAll
import org.ouraircraft.model.data.valueclasses.AircraftRegistration

private const val CONSTRAINT_NAME = "valid_registration"

fun Column<String>.asAircraftRegistration(): Column<AircraftRegistration> =
    check(CONSTRAINT_NAME) { column ->
        object : Op<Boolean>() {
            override fun toQueryBuilder(queryBuilder: QueryBuilder) {
                queryBuilder {
                    appendAll("length(", column, ") > 0")
                    append(" AND ")
                    appendAll(column, " NOT GLOB '*[^A-Z0-9]*'")
                }
            }
        }
    }.transform(
        wrap = ::AircraftRegistration,
        unwrap = AircraftRegistration::value
    )