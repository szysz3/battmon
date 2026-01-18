package com.battmon.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

interface ApcAccessClient {
    suspend fun fetchStatusOutput(): String
}

class ProcessApcAccessClient(
    private val command: String,
    private val timeoutSeconds: Long = DEFAULT_TIMEOUT_SECONDS
) : ApcAccessClient {
    private val logger = LoggerFactory.getLogger(ProcessApcAccessClient::class.java)

    override suspend fun fetchStatusOutput(): String = coroutineScope {
        val commandParts = parseCommand(command)
        val process = ProcessBuilder(commandParts)
            .redirectErrorStream(true)
            .start()

        try {
            val outputDeferred = async(Dispatchers.IO) {
                process.inputStream.bufferedReader().use { reader ->
                    reader.readText()
                }
            }

            val completed = withContext(Dispatchers.IO) {
                process.waitFor(timeoutSeconds, TimeUnit.SECONDS)
            }

            if (!completed) {
                logger.warn("apcaccess timed out after ${timeoutSeconds}s")
                process.destroy()
                if (!process.waitFor(2, TimeUnit.SECONDS)) {
                    process.destroyForcibly()
                }
                outputDeferred.cancel()
                throw IllegalStateException("apcaccess timed out after ${timeoutSeconds}s")
            }

            val output = outputDeferred.await()
            val exitCode = process.exitValue()
            if (exitCode != 0) {
                throw IllegalStateException("apcaccess failed (exit=$exitCode): ${output.trim()}")
            }
            output
        } finally {
            if (process.isAlive) {
                process.destroy()
                if (!process.waitFor(2, TimeUnit.SECONDS)) {
                    process.destroyForcibly()
                }
            }
        }
    }

    private fun parseCommand(command: String): List<String> {
        val tokenRegex = Regex("""("[^"]*"|'[^']*'|\S+)""")
        return tokenRegex.findAll(command)
            .map { match ->
                match.value.trim().removeSurrounding("\"").removeSurrounding("'")
            }
            .toList()
            .ifEmpty { listOf(command) }
    }

    companion object {
        private const val DEFAULT_TIMEOUT_SECONDS = 7L
    }
}
