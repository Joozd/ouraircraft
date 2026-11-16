package org.ouraircraft.model.data

/**
 * A page of results from a larger result set.
 *
 * @param T The type of each result.
 * @property results The results on this page.
 * @property limit The requested maximum number of results per page.
 * @property offset The zero-based number of results skipped before this page.
 * @property totalCount The total number of matching results before pagination.
 */
data class PagedResult<T>(
    val results: List<T>,
    val limit: Int,
    val offset: Long,
    val totalCount: Long,
)