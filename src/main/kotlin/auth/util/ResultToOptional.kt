package org.ouraircraft.auth.util

import java.util.Optional

/**
 * makes an Optional from a Result.
 */
inline fun <reified T: Any> Result<T>.toOptional(): Optional<T> {
    onFailure {
        return Optional.empty<T>()
    }
    return Optional.ofNullable(getOrThrow())
}

/**
 * makes an Optional from a Result.
 */
inline fun <reified R, T: Any> Result<R>.mapToOptional(map: (R) -> T?): Optional<T> {
    onFailure {
        return Optional.empty<T>()
    }
    return Optional.ofNullable(map(getOrThrow()))
}