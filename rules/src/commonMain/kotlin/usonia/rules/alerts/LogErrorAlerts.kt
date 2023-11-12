package usonia.rules.alerts

import kimchi.logger.LogLevel
import kimchi.logger.LogWriter
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import regolith.processes.daemon.Daemon
import usonia.core.client.alertAll
import usonia.foundation.Action
import usonia.kotlin.*
import usonia.rules.Flags
import usonia.server.client.BackendClient

/**
 * Send Error logs to debug users as telegram messages.
 */
object LogErrorAlerts: Daemon, LogWriter {
    private val logs = MutableSharedFlow<String>(extraBufferCapacity = 5, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    internal val client = MutableStateFlow<BackendClient?>(null)

    override fun log(level: LogLevel, message: String, cause: Throwable?) {
        logs.tryEmit(message)
    }

    override fun shouldLog(level: LogLevel, cause: Throwable?): Boolean {
        return level >= LogLevel.ERROR
    }

    override suspend fun startDaemon(): Nothing {
        client
            .filterNotNull()
            .asOngoing()
            .flatMapLatest { client ->
                client.flags.map { client to it[Flags.LogAlerts] }.asFlow()
            }
            .flatMapLatest { (client, enabled) ->
                if (enabled.toBoolean()) logs.map { client to it }
                else emptyFlow()
            }
            .collect { (client, log) ->
                client.alertAll(
                    message = "Error: $log",
                    level = Action.Alert.Level.Debug,
                    icon = Action.Alert.Icon.Danger,
                )
            }
    }
}
