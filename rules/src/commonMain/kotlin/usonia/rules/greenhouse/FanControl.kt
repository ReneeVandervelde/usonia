package usonia.rules.greenhouse

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.CoroutineScope
import usonia.core.state.publishAll
import usonia.foundation.*
import usonia.kotlin.*
import usonia.server.Daemon
import usonia.server.client.BackendClient

const val DEFAULT_HIGH_BOUND = 80
const val DEFAULT_UPPER_BUFFER = 3

/**
 * Turns fans on/off based on an upper-limit temperature.
 */
class FanControl(
    private val client: BackendClient,
    private val logger: KimchiLogger = EmptyLogger,
    private val backgroundScope: CoroutineScope = DefaultScope(),
): Daemon {
    override suspend fun start(): Nothing {
        client.site.collectLatest { site ->
            client.events
                .filterIsInstance<Event.Temperature>()
                .filter { event -> site.findRoomContainingDevice(event.source)?.type == Room.Type.Greenhouse }
                .collectOn(backgroundScope) { event -> onTemperature(site, event) }
        }
    }

    private suspend fun onTemperature(site: Site, event: Event.Temperature) {
        val device = site.findDevice(event.source) ?: run {
            logger.warn("Unable to find device with ID `${event.source}` for fan control.")
            return
        }
        val room = site.findRoomContainingDevice(event.source) ?: run {
            logger.warn("Unable to find room containing device `${event.source}` for fan control.")
            return
        }
        when {
            event.temperature >= DEFAULT_HIGH_BOUND -> {
                logger.info("Temperature of <${device.name}> exceeds upper greenhouse bound. Currently ${event.temperature}ºF")
                switchFans(room, SwitchState.ON)
            }
            event.temperature < DEFAULT_HIGH_BOUND - DEFAULT_UPPER_BUFFER -> {
                logger.trace("Temperature of <${device.name}> is within bounds <${event.temperature}ºF>")
                switchFans(room, SwitchState.OFF)
            }
            else -> {
                logger.trace("Temperature is within buffer range <${event.temperature}ºF>. Taking No action.")
            }
        }
    }

    private suspend fun switchFans(room: Room, state: SwitchState) {
        room.devices
            .filter { it.fixture == Fixture.Fan }
            .filter { Action.Switch::class in it.capabilities.actions }
            .map {
                Action.Switch(
                    target = it.id,
                    state = state,
                )
            }
            .run { client.publishAll(this) }
    }
}
