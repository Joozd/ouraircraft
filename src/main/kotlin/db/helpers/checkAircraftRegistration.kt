package org.ouraircraft.db.helpers

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.QueryBuilder
import org.jetbrains.exposed.v1.core.Table
import org.ouraircraft.db.utils.appendAll
import org.ouraircraft.model.data.valueclasses.AircraftRegistration

private const val CONSTRAINT_NAME = "valid_registration"

context(table: Table)
fun Column<String>.asAircraftRegistration(): Column<AircraftRegistration> = with(table) {
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
}