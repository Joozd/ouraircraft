package org.ouraircraft.model.dto.convertors

import org.ouraircraft.model.data.PagedResult
import org.ouraircraft.model.dto.data.PagedResultDto

/**
 * Converts this page to a DTO, transforming each result with [typeConvertor].
 *
 * Preserves the limit, offset, total count, and result order.
 *
 * example: `val dto = pagedResult.toDto { it.toUncheckedDto() }`
 *
 * @param T The original result type.
 * @param R The converted result type.
 * @param typeConvertor Converts an individual result to its DTO representation.
 * @return A DTO containing the converted results and the original pagination metadata.
 */
fun <T, R> PagedResult<T>.toDto(typeConvertor: (T) -> R) = PagedResultDto(
    results = results.map(typeConvertor),
    limit = limit,
    offset = offset,
    totalCount = totalCount
)