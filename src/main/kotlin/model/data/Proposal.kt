package org.ouraircraft.model.data

import org.ouraircraft.model.data.Proposal.Type.*
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Represents a proposal to create, modify, or delete a database record.
 *
 * The concrete subclass determines the type of record and contains the proposed data.
 *
 * @property id Internal ID of this proposal. Disregarded when creating a new proposal;
 *   used to identify an existing proposal when reviewing it.
 * @property targetId ID of the record to modify or delete, or null when proposing a new record.
 *   The concrete subclass determines which type of record this ID refers to.
 * @property resultId ID of the record created or updated after approval,
 *   or null if pending, rejected, or approved for deletion.
 *   The concrete subclass determines which type of record this ID refers to.
 * @property authorId ID of the user who submitted the proposal.
 * @property delete Whether this proposal requests deletion of the target record.
 *   Requires a non-null [targetId].
 * @property proposalComment Optional explanation provided by the author.
 * @property createdAt Timestamp when the proposal was submitted.
 * @property checkedBy ID of the user who reviewed the proposal, or null if pending.
 * @property checkResult Review decision, or null if pending.
 * @property checkResultComment Optional explanation provided by the reviewer.
 * @property checkedAt Timestamp when the proposal was reviewed, or null if pending.
 */
sealed interface Proposal{
    val id: Long
    val targetId: Long?
    val resultId: Long?
    val authorId: Uuid
    val delete: Boolean
    val proposalComment: String?
    val createdAt: Instant
    val checkedBy: Uuid?
    val checkResult: CheckResult?
    val checkResultComment: String?
    val checkedAt: Instant?

    /**
     * Enforce domain rules for a Proposal
     */
    fun checkSanity() {
        if (delete && targetId == null) throw IllegalStateException("A Proposal to delete needs a target ID to delete")
    }


    /**
     * Returns the [Type] of this proposal for routing to the appropriate handler.
     *
     * An existing [Proposal.checkResult] is final and takes precedence over [checkResult].
     *
     * @param checkResult Review decision to use if this proposal has no stored decision yet.
     *   Does not modify the proposal.
     * @return [Type.PENDING] if neither decision is available, [Type.REJECTED] if rejected,
     *   or [Type.NEW], [Type.UPDATE], or [Type.DELETE] if approved.
     */
    fun type(checkResult: CheckResult? = null): Type {
        checkSanity() // enforce domain assumptions (e.g. delete requires a non-null target)
        val combinedCheckResult = this.checkResult ?: checkResult ?: return PENDING
        when(combinedCheckResult){ // using a when here to force exhausting all CheckResults
            CheckResult.REJECTED -> return REJECTED
            CheckResult.APPROVED -> {
                if(targetId == null) return NEW
                if(delete) return DELETE
                return UPDATE
            }
        }
    }

    enum class Type {
        NEW,
        UPDATE,
        DELETE,
        REJECTED,
        PENDING;
    }
}