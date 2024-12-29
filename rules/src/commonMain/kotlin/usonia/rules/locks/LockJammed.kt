package usonia.rules.locks

import com.inkapplications.coroutines.ongoing.*
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import regolith.processes.daemon.Daemon
import usonia.core.client.alertAll
import usonia.core.state.findDevice
import usonia.foundation.Action
import usonia.foundation.Event
import usonia.foundation.LockState
import usonia.server.client.BackendClient

internal class LockJammed(
    private val client: BackendClient,
    private val logger: KimchiLogger = EmptyLogger,
): Daemon {
    override suspend fun startDaemon(): Nothing {
        client.site.collectLatest { site ->
            client.events
                .filterIsInstance<Event.Lock>()
                .filter { it.state == LockState.UNKNOWN }
                .mapLatest { client.findDevice(it.source) }
                .filterNotNull()
                .collect { lock ->
                    client.alertAll(
                        message = "${lock.name} did not lock properly",
                        level = Action.Alert.Level.Warning,
                        icon = Action.Alert.Icon.Panic,
                    )
                }
        }
    }
}
