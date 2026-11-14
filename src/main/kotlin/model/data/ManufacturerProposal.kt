package org.ouraircraft.model.data

import org.ouraircraft.model.auth.User
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Represents a proposal to create, modify, or delete a manufacturer.
 *
 * To create a new proposal, use the secondary constructor
 *
 * Contains a complete snapshot of the proposed manufacturer data.
 * Once created, proposal data is immutable. Review fields may be populated
 * once when the proposal is accepted or rejected.
 *
 * @property id Unique identifier of this proposal.
 * @property targetId ID of the manufacturer being modified or deleted,
 *   or null if proposing a new manufacturer.
 * @property resultId ID of the resulting manufacturer after acceptance,
 *   or null if pending or rejected.
 * @property authorId ID of the user who submitted the proposal.
 * @property name Proposed manufacturer name.
 * @property longName Optional full manufacturer name.
 * @property delete Whether this proposal requests deletion of an existing manufacturer.
 * @property proposalComment Optional explanation provided by the author.
 * @property createdAt Timestamp when the proposal was submitted.
 * @property checkedBy ID of the reviewing user, or null if pending.
 * @property checkResult Review decision, or null if pending.
 * @property checkResultComment Optional explanation provided by the reviewer.
 * @property checkedAt Timestamp when the proposal was reviewed, or null if pending.
 */
data class ManufacturerProposal(
    override val id: Long,
    override val targetId: Long?,
    override val resultId: Long?,
    override val authorId: Uuid,
    override val delete: Boolean,
    override val proposalComment: String?,
    override val createdAt: Instant,
    override val checkedBy: Uuid?,
    override val checkResult: CheckResult?,
    override val checkResultComment: String?,
    override val checkedAt: Instant?,

    val name: String,
    val longName: String?,
): Proposal {
    constructor(
        user: User,
        targetId: Long?,
        delete: Boolean,
        proposalComment: String?,
        name: String,
        longName: String?
    ): this(
        id = -1,
        targetId = targetId,
        resultId = null,
        authorId = user.id,
        delete = delete,
        proposalComment = proposalComment,
        createdAt = Clock.System.now(),
        checkedBy = null,
        checkResult = null,
        checkResultComment = null,
        checkedAt = null,
        name = name,
        longName = longName
    )

    init{
        checkSanity()
    }
}