package usonia.rules.alerts

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import usonia.core.Daemon
import usonia.core.state.*
import usonia.foundation.Action
import usonia.foundation.Event
import usonia.foundation.User
import usonia.foundation.Identifier
import usonia.foundation.WaterState.*
import usonia.kotlin.neverEnding

/**
 * Send out an Alert when water is detected by any water sensor.
 */
internal class WaterMonitor(
    private val configurationAccess: ConfigurationAccess,
    private val eventAccess: EventAccess,
    private val actionPublisher: ActionPublisher,
    private val logger: KimchiLogger = EmptyLogger
): Daemon {
    private val alerted = mutableSetOf<Identifier>()

    override suspend fun start() = neverEnding {
        eventAccess.events
            .filterIsInstance<Event.Water>()
            .collect {
                when (it.state) {
                    WET -> onWet(it)
                    DRY -> onDry(it)
                }
            }
    }

    private fun onDry(event: Event.Water) {
        if (event.source !in alerted) run {
            logger.debug("Duplicate dry report for device: <${event.source}>")
            return
        }
        logger.debug("Removing <${event.source}> from water alerted list.")
        alerted.remove(event.source)
    }

    private suspend fun onWet(event: Event.Water) {
        if (event.source in alerted) run {
            logger.debug("Alert already sent. Ignoring wet event.")
            return
        }
        alerted.add(event.source)
        val device = configurationAccess.getDeviceById(event.source) ?: run {
            logger.error("Unable to find device with ID: <${event.source}>")
            sendAlerts("<Device: ${event.source}>")
            return
        }
        sendAlerts(device.name)
    }

    private suspend fun sendAlerts(deviceName: String) {
        configurationAccess.getSite().users
            .also { logger.trace("Sending Water alerts to: [${it.joinToString { it.name }}]") }
            .forEach { user -> sendAlert(user, deviceName) }
    }

    private suspend fun sendAlert(user: User, deviceName: String) {
        actionPublisher.publishAction(Action.Alert(
            target = user.id,
            message = "Water detected by $deviceName!"
        ))
    }
}
