package usonia.frontend

import kimchi.logger.LogLevel
import kimchi.logger.LogWriter
import kotlinx.coroutines.channels.*
import usonia.server.WebSocketController

class LogSocket: WebSocketController, LogWriter {
    override val path: String = "/logs"
    private val logs = BroadcastChannel<String>(Channel.BUFFERED)

    override suspend fun start(input: ReceiveChannel<String>, output: SendChannel<String>) {
        logs.openSubscription().consumeEach {
            output.send(it)
        }
    }

    override fun log(level: LogLevel, message: String, cause: Throwable?) {
        logs.offer("${level.name}: $message")
    }

    override fun shouldLog(level: LogLevel, cause: Throwable?): Boolean = true
}
