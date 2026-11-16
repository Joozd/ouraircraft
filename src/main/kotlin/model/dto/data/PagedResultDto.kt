package org.ouraircraft.model.dto.data

import kotlinx.serialization.Serializable

@Serializable
data class PagedResultDto<T>(
    val results: List<T>,
    val limit: Int,
    val offset: Long,
    val totalCount: Long,
)
