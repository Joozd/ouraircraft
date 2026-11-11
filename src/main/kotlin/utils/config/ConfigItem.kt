package org.ouraircraft.utils.config

import java.nio.file.Path

sealed interface ConfigItem <T>{
    val value: T
}

// items with values:
data class DatabaseLocation(override val value: Path) : ConfigItem<Path>




fun parseConfigLine(line: String): Pair<ConfigItemName, ConfigItem<*>> {
    val (key, value) = line.split("=", limit = 2).map { it.trim() }
    return when (key) {
        "databaseLocation" -> ConfigItemName.DATABASE_LOCATION to DatabaseLocation(Path.of(value))
        else -> throw IllegalArgumentException("Unknown config key: $key")
    }
}
