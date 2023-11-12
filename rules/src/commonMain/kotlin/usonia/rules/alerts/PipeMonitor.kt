package usonia.rules.alerts

import inkapplications.spondee.measure.us.toFahrenheit
import inkapplications.spondee.structure.toFloat
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import regolith.processes.daemon.Daemon
import usonia.core.client.alertAll
import usonia.foundation.*
import usonia.kotlin.*
import usonia.server.client.BackendClient

/**
 * Send alerts if a pipe temperature monitor gets near-freezing.
 */
class PipeMonitor(
    private val client: BackendClient,
    private val logger: KimchiLogger = EmptyLogger,
    private val backgroundScope: CoroutineScope = DefaultScope(),
): Daemon {
    private val alerted = mutableSetOf<Identifier>()

    override suspend fun startDaemon(): Nothing {
        client.site.collectLatest { site ->
            coroutineScope {
                launch {
                    client.events
                        .filterIsInstance<Event.Temperature>()
                        .filter { it.temperature.toFahrenheit().toFloat() < 38 }
                        .map { site.findDevice(it.source) to it }
                        .filter { (device, _) -> device?.fixture == Fixture.Pipe }
                        .collectOn(backgroundScope) { (device, event) ->
                            sendAlert(device!!, event)
                        }
                }
                launch {
                    client.events
                        .filterIsInstance<Event.Temperature>()
                        .filter { it.temperature.toFahrenheit().toFloat() > 40 }
                        .map { site.findDevice(it.source) }
                        .filter { device -> device?.fixture == Fixture.Pipe }
                        .collect { device -> reset(device!!) }
                }
            }
        }
    }

    private suspend fun sendAlert(device: Device, event: Event.Temperature) {
        if (device.id in alerted) {
            logger.trace("Duplicate alert for ${device.name}")
            return
        }

        logger.info("Sending Water alert")
        client.alertAll(
            message = "${device.name} is down to ${event.temperature}º!",
            level = Action.Alert.Level.Warning,
            icon = Action.Alert.Icon.Pipes,
        )

        alerted += device.id
    }

    private fun reset(device: Device) {
        if (device.id !in alerted) {
            return
        }
        logger.info("Resetting Pipe Alerts for ${device.name}")
        alerted -= device.id
    }
}
