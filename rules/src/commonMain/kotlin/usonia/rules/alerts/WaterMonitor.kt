package usonia.rules.alerts

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import usonia.core.client.alertAll
import usonia.core.state.*
import usonia.foundation.Action
import usonia.foundation.Event
import usonia.foundation.Identifier
import usonia.foundation.WaterState.*
import usonia.kotlin.DefaultScope
import usonia.kotlin.neverEnding
import usonia.server.Daemon
import usonia.server.client.BackendClient

/**
 * Send out an Alert when water is detected by any water sensor.
 */
internal class WaterMonitor(
    private val client: BackendClient,
    private val logger: KimchiLogger = EmptyLogger,
    private val backgroundScope: CoroutineScope = DefaultScope(),
): Daemon {
    private val alerted = mutableSetOf<Identifier>()

    override suspend fun start() = neverEnding {
        client.events
            .filterIsInstance<Event.Water>()
            .collect {
                backgroundScope.launch {
                    when (it.state) {
                        WET -> onWet(it)
                        DRY -> onDry(it)
                    }
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
        val name = client.findDevice(event.source)?.name ?: "<Device: ${event.source}>".also {
            logger.error("Unable to find device with ID: <${event.source}>")
        }
        logger.trace("Sending out alerts for <$name>")
        client.alertAll("Water detected by $name!", Action.Alert.Level.Emergency)
    }
}
