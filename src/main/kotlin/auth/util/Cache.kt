package org.ouraircraft.auth.util

import com.github.benmanes.caffeine.cache.Caffeine
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

/**
 * Cache to hold data of type [V] for a duration of [timeOut].
 * Data can be retrieved by its key of type [K].
 * This cache is backed by a [Caffeine] cache, and can be accessed as a [MutableMap].
 */
internal fun <K: Any, V: Any> buildCache(timeOut: Duration, maximumSize: Long = 10_000) = Caffeine.newBuilder()
        .expireAfterWrite(timeOut.inWholeSeconds, TimeUnit.SECONDS)
        .maximumSize(maximumSize)
        .build<K,V>()
        .asMap()