package org.ouraircraft.db.exceptions

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import java.util.concurrent.CancellationException

/**
 * Coroutines-friendly version of runCatching that maps ExposedSQLException to domain exceptions.
 * If the exception is a CancellationException, it is rethrown to allow proper coroutine cancellation
 */
internal inline fun <T> runCatchingAndMapToDomainException(block: () -> T): Result<T> =
    runCatching(block).fold(
        onSuccess = { Result.success(it) },
        onFailure = { exception ->
            if (exception is CancellationException) throw exception

            Result.failure(
                (exception as? ExposedSQLException)
                    ?.toDomainExceptionOrNull() ?: exception
            )
        }
    )