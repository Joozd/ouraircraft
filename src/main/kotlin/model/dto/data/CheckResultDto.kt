package org.ouraircraft.model.dto.data

import kotlinx.serialization.Serializable
import org.ouraircraft.model.data.CheckResult

/**
 * The result of checking a proposal
 */
@Serializable
data class CheckResultDto(
    val checkResult: CheckResult,
    val checkResultComment: String?,
)
