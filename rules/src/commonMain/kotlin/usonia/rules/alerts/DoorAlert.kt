package usonia.rules.alerts

import com.inkapplications.coroutines.ongoing.*
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import regolith.processes.daemon.Daemon
import usonia.core.client.alertAll
import usonia.core.state.findDevice
import usonia.foundation.*
import usonia.server.client.BackendClient
import kotlin.time.Duration.Companion.seconds

/**
 * Send an alert if a door is opened while all users are away.
 */
class DoorAlert(
    private val client: BackendClient,
    private val logger: KimchiLogger = EmptyLogger,
): Daemon {
    override suspend fun startDaemon(): Nothing {
        client.securityState
            .flatMapLatest {
                if (it == SecurityState.Armed) client.events.asFlow()
                else emptyFlow()
            }
            .filterIsInstance<Event.Latch>()
            .filter { it.state == LatchState.OPEN }
            .mapLatest { client.findDevice(it.source) }
            .filterNotNull()
            .filter { Fixture.EntryPoint == it.fixture }
            .collectLatest { device ->
                // Delay to allow for potential disarm events to be processed before alerting.
                delay(5.seconds)
                client.alertAll(
                    message = "${device.name} was opened while you were gone!",
                    level = Action.Alert.Level.Warning,
                    icon = Action.Alert.Icon.Suspicious,
                )
            }
    }
}
