package usonia.rules.alerts

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import usonia.core.client.alertAll
import usonia.core.state.allAway
import usonia.core.state.findDevice
import usonia.foundation.Action
import usonia.foundation.Event
import usonia.foundation.Fixture
import usonia.foundation.LatchState
import usonia.kotlin.*
import usonia.server.Daemon
import usonia.server.client.BackendClient

/**
 * Send an alert if a door is opened while all users are away.
 */
class DoorAlert(
    private val client: BackendClient,
    private val logger: KimchiLogger = EmptyLogger,
): Daemon {
    override suspend fun start(): Nothing {
        client.site.collectLatest { site ->
            client.events
                .filterIsInstance<Event.Latch>()
                .filter { it.state == LatchState.OPEN }
                .mapLatest { client.findDevice(it.source) }
                .filterNotNull()
                .filter { Fixture.EntryPoint == it.fixture }
                .collectLatest {  device ->
                    if (client.allAway(site.users)) {
                        client.alertAll("${device.name} was opened while you were gone!", Action.Alert.Level.Warning)
                    }
                }
        }
    }
}
