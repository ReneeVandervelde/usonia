package usonia.web

import kimchi.logger.LogLevel
import kimchi.logger.LogWriter
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import usonia.foundation.LogMessage
import usonia.server.http.WebSocketController
import kotlin.math.max

object LogSocket: WebSocketController, LogWriter {
    override val path: String = "/logs"
    private val logs = MutableSharedFlow<LogMessage>(
        replay = 500,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override suspend fun start(
        input: ReceiveChannel<String>,
        output: SendChannel<String>,
        parameters: Map<String, List<String>>,
    ) {
        val overflow = max(logs.replayCache.size - (parameters["bufferCount"]?.first()?.toInt() ?: 0), 0)
        logs.drop(overflow)
            .collect {
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
