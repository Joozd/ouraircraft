package org.ouraircraft

class CliArgs(args: Array<String>) {
    private val normalizedArgs = args.map { normalizeArg(it) }

    val inMemoryDb = IN_MEMORY_DB in args
    val configFileLocation: String? = getArg(CONFIG_FILE)
    val hostName = getArg(HOSTNAME)



    override fun toString() = "CliArgs(inMemoryDb = $inMemoryDb, configFileLocation = $configFileLocation)"


    /**
     * Gets the (single) value for [argName]
     * example: -location /foo/bar in CLI would give "/foo/bar" for key "-location"
     */
    private fun getArg(argName: String): String? {
        val argIndex = normalizedArgs.indexOfFirst { it == argName }
        if(argIndex == -1) return null
        val value = normalizedArgs.getOrNull(argIndex + 1)
            ?: throw CliFormatException("$CONFIG_FILE argument given without parameter")

        if (value.startsWith("-"))
            throw CliFormatException("Argument of $CONFIG_FILE must not start with '-'")

        return value
    }



    private fun normalizeArg(arg: String): String =
        arg.lowercase()

    companion object{
        const val IN_MEMORY_DB = "--in-memory-db"
        const val CONFIG_FILE = "-config-file"
        const val HOSTNAME = "-hostname"
    }

    class CliFormatException(message: String): RuntimeException(message)
}