package usonia.web

import kimchi.logger.LogLevel
import kimchi.logger.LogWriter
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import usonia.core.server.WebSocketController
import usonia.foundation.LogMessage

object LogSocket: WebSocketController, LogWriter {
    override val path: String = "/logs"
    private val logs = MutableSharedFlow<LogMessage>(
        replay = 1000,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override suspend fun start(input: ReceiveChannel<String>, output: SendChannel<String>) {
        logs.collect {
            output.send(Json.encodeToString(it))
        }
    }

    override fun log(level: LogLevel, message: String, cause: Throwable?) {
        val logMessage = LogMessage(
            level = level,
            message = message,
            stackTrace = cause?.stackTraceToString(),
        )
        logs.tryEmit(logMessage)
    }

    override fun shouldLog(level: LogLevel, cause: Throwable?): Boolean = true
}
