package org.ouraircraft.utils.config

abstract class Config {
    abstract operator fun <T> get(configValue: ConfigItem<T>): T
}