package org.ouraircraft.db.exceptions

import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.sqlite.SQLiteException

/**
 * If there is a known mapping from the ExposedSQLException to a domain exception, return it.
 * Otherwise, return null.
 */
internal fun ExposedSQLException.toDomainExceptionOrNull(): Exception?{
    return when(cause){
        is SQLiteException -> (cause as SQLiteException).toDomainExceptionOrNull()
        else -> null
    }
}

private fun SQLiteException.toDomainExceptionOrNull(): Exception?{
    when(resultCode?.code){
        1811 -> return domainConstraintTriggerOrNull()
        2067 -> return DuplicateEntryException("Unique constraint violation", this)
    }
    return null
}

private fun SQLiteException.domainConstraintTriggerOrNull(): Exception? =
    message?.let { m ->
        when {
            m.contains(RaiseMessages.RAISE_MESSAGE_APPEND_ONLY) -> AppendOnlyException(RaiseMessages.RAISE_MESSAGE_APPEND_ONLY, this)
            else -> null
        }
    }
