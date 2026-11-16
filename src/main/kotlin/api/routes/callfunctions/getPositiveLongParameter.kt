package org.ouraircraft.api.routes.callfunctions

import io.ktor.server.application.ApplicationCall
import kotlin.text.toLongOrNull

/**
 * Returns the value of the parameter, [default] if no paramater provided, or null if bad data or no default and no data
 */
internal fun ApplicationCall.getPositiveLongParameter(name: String, default: Long? = null): Long? =
    request.queryParameters[name]?.let {
        it.toLongOrNull()?.takeIf { value -> value >= 0 }
    } ?: default