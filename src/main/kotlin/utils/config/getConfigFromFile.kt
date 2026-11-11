package org.ouraircraft.utils.config

import java.io.File

fun getConfigFromFile(filePath: String): Map<ConfigItemName, ConfigItem<*>> {
    val file = File(filePath)

    if (!file.exists()) {
        throw IllegalArgumentException("Configuration file not found at path: $filePath")
    }

    return file.readLines().mapNotNull { line ->
        val trimmedLine = line.trim()
        if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("#")) {
            parseConfigLine(trimmedLine)
        } else null
    }.toMap()
}