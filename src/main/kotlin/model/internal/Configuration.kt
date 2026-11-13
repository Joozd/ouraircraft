package org.ouraircraft.model.internal

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

/**
 * This holds the complete configuration of the application.
 */
data class Configuration(
    val programName: String = "OurAircraft",
    // When required, this can be split into subparts
    val inMemoryDb: Boolean,
    val configFileLocation: String?,
    val hostName: String?,

    val userSessionConfig: UserSessionConfig = UserSessionConfig(),
    val dataRetrievalConfig: DataRetrievalConfig = DataRetrievalConfig(),
) {
    /**
     * This holds all configuration regarding Users being logged in.
     */
    data class UserSessionConfig(
        val sessionTTLExtensionDuration: Duration = 30.days,
        val passkeySessionTTL: Duration = 30.minutes,
        val userSessionTTL: Duration = 30.days,
        val maxExtendedSessionTTL: Duration = 365.days * 1000, // 1000 years means no practical max extension for now
        val recoveryTokenTTl: Duration = 30.minutes,
    )

    data class DataRetrievalConfig(
        val matchesPerPage: Int = 20
    )
}