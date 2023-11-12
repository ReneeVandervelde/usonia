package usonia.rules.charging

import inkapplications.spondee.measure.Power
import inkapplications.spondee.measure.metric.watts
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import regolith.processes.daemon.Daemon
import usonia.foundation.*
import usonia.kotlin.collectLatest
import usonia.kotlin.filter
import usonia.kotlin.filterIsInstance
import usonia.kotlin.rollingWindow
import usonia.server.client.BackendClient

/**
 * Number of consecutive events to be considered a declining charge rate.
 */
private const val REQUIRED_CONSECUTIVE_EVENTS = 4

/**
 * Shut off a charger as soon as the output begins to decrease.
 *
 * This is a crude way to prevent a battery from being fully charged to 100%.
 */
class PowerLimitCharge(
    private val backendClient: BackendClient,
    private val logger: KimchiLogger = EmptyLogger,
): Daemon {
    override suspend fun startDaemon(): Nothing {
        backendClient.site.collectLatest { site ->
            backendClient.events
                .filterIsInstance<Event.Power>()
                .filter { it.power != 0.watts }
                .filter { Fixture.Charger == site.findDevice(it.source)?.fixture }
                .filter { Action.Switch::class in site.findDevice(it.source)?.capabilities?.actions.orEmpty() }
                .rollingWindow(REQUIRED_CONSECUTIVE_EVENTS)
                .collectLatest { analyze(it) }
        }
    }

    private suspend fun analyze(events: List<Event.Power>) {
        events
            .map { it.power }
            .scan(null as Power?) { last, next ->
                if (last != null && last.toWatts().value.toDouble() <= next.toWatts().value.toDouble()) return
                next
            }

        logger.info("Shutting off charger with consecutive decreased wattage events")
        backendClient.publishAction(Action.Switch(
            target = events.first().source,
            state = SwitchState.OFF,
        ))
    }
}
