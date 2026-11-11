package org.ouraircraft.db.exceptions

/**
 * Indicates that an entry could not be created because it would violate
 * a uniqueness constraint.
 *
 * @param reason Description of the constraint violation.
 * @param cause The underlying exception that caused the failure, if any.
 */
open class DuplicateEntryException(
    reason: String,
    cause: Throwable? = null
) : IllegalStateException(reason, cause)

class UserAlreadyExistsException(val screenName: String): DuplicateEntryException("User with name $screenName already exists")
class EmailAlreadyInUseException(val email: String): DuplicateEntryException("Email $email is already in use")
