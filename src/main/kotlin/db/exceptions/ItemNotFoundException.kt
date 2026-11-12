@file:Suppress("CanBeParameter")

package org.ouraircraft.db.exceptions

import kotlin.uuid.Uuid

sealed class ItemNotFoundException(message: String? = null): NoSuchElementException(message)

class UserNotFoundByScreenNameException(val screenName: String): ItemNotFoundException("User with name $screenName not found")
class UserNotFoundByEmailException(val email: String): ItemNotFoundException("User with email $email not found")
class UserNotFoundByIDException(val userId: Uuid): ItemNotFoundException("User with ID $userId not found")
class PendingUserNotFoundException(val userId: Uuid): ItemNotFoundException("Pending User with ID $userId not found")
class SessionNotFoundByIdException(val sessionId: Long): ItemNotFoundException("Session with ID $sessionId not found in Database.")
class ProposalNotFoundByIdException(val proposalId: Long): ItemNotFoundException("Proposal with ID $proposalId not found in Database.")
class TargetItemForProposalNotFoundByIdException(val targetId: Long, val proposalId: Long): ItemNotFoundException("Target item with id $targetId not found in database, from Proposal with ID $proposalId.")

