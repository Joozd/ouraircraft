package org.ouraircraft.db.exceptions

import java.io.IOException

/**
 * This exception is thrown when a delete or update operation is attempted on a Table that is append-only.
 */
class AppendOnlyException(reason: String, cause: Throwable?): IOException(reason, cause)