package usonia.rules.alerts

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import usonia.foundation.*
import usonia.kotlin.neverEnding
import usonia.server.Daemon
import usonia.server.client.BackendClient

/**
 * Send alerts if a pipe temperature monitor gets near-freezing.
 */
class PipeMonitor(
    private val client: BackendClient,
    private val logger: KimchiLogger = EmptyLogger,
): Daemon {
    private val alerted = mutableSetOf<Identifier>()

    override suspend fun start(): Nothing = neverEnding {
        client.site.collectLatest { site ->
            coroutineScope {
                launch {
                    client.events
                        .filterIsInstance<Event.Temperature>()
                        .filter { it.temperature < 38 }
                        .map { site.findDevice(it.source) to it }
                        .filter { (device, _) -> device?.fixture == Fixture.Pipe }
                        .collect { (device, event) ->
                            sendAlert(site, device!!, event)
                        }
                }
                launch {
                    client.events
                        .filterIsInstance<Event.Temperature>()
                        .filter { it.temperature > 40 }
                        .map { site.findDevice(it.source) }
                        .filter { device -> device?.fixture == Fixture.Pipe }
                        .collect { device -> reset(device!!) }
                }
            }
        }
    }

    private suspend fun sendAlert(site: Site, device: Device, event: Event.Temperature) {
        if (device.id in alerted) {
            logger.trace("Duplicate alert for ${device.name}")
            return
        }

        site.users
            .also { logger.trace("Sending Water alerts to: [${it.joinToString { it.name }}]") }
            .forEach { user ->
                client.publishAction(Action.Alert(
                    target = user.id,
                    message = "${device.name} is down to ${event.temperature}º!"
                ))
            }

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
