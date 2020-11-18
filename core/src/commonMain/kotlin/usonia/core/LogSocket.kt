package usonia.core

import kimchi.logger.LogLevel
import kimchi.logger.LogWriter
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import usonia.server.WebSocketController

object LogSocket: WebSocketController, LogWriter {
    override val path: String = "/logs"
    private val logs = MutableSharedFlow<String>(
        replay = 1000,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override suspend fun start(input: ReceiveChannel<String>, output: SendChannel<String>) {
        logs.collect {
            output.send(it)
        }
    }

    override fun log(level: LogLevel, message: String, cause: Throwable?) {
        logs.tryEmit("${level.name}: $message")
        cause?.stackTraceToString()?.run(logs::tryEmit)
    }

    override fun shouldLog(level: LogLevel, cause: Throwable?): Boolean = true
}
