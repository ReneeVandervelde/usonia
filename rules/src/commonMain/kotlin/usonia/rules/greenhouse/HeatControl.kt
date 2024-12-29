package usonia.rules.greenhouse

import com.inkapplications.coroutines.ongoing.collectLatest
import com.inkapplications.coroutines.ongoing.collectOn
import com.inkapplications.coroutines.ongoing.filter
import com.inkapplications.coroutines.ongoing.filterIsInstance
import inkapplications.spondee.measure.us.toFahrenheit
import inkapplications.spondee.structure.toFloat
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.*
import regolith.processes.daemon.Daemon
import regolith.processes.daemon.DaemonFailureHandler
import regolith.processes.daemon.DaemonRunAttempt
import regolith.processes.daemon.FailureSignal
import usonia.core.state.getSecurityState
import usonia.core.state.publishAll
import usonia.foundation.*
import usonia.kotlin.DefaultScope
import usonia.server.client.BackendClient
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

private const val DEFAULT_LOW_BOUND = 73
private const val DEFAULT_LOW_BUFFER = 3
private const val AWAY_TEMP_SHIFT = 8

/**
 * Turns heaters on/off based on an lower-limit temperature.
 */
class HeatControl(
    private val client: BackendClient,
    private val failureHandler: DaemonFailureHandler,
    private val logger: KimchiLogger = EmptyLogger,
    private val backgroundScope: CoroutineScope = DefaultScope(),
): Daemon {
    private val heatingTimeouts = ConcurrentHashMap<Room, Job?>()

    override suspend fun onFailure(attempts: List<DaemonRunAttempt>): FailureSignal {
        return failureHandler.onFailure(attempts)
    }

    override suspend fun startDaemon(): Nothing {
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
        val securityState = client.getSecurityState()
        val onPoint = if (securityState == SecurityState.Armed) {
            DEFAULT_LOW_BOUND - AWAY_TEMP_SHIFT
        } else {
            DEFAULT_LOW_BOUND
        }
        val offPoint = if (securityState == SecurityState.Armed) {
            DEFAULT_LOW_BOUND + DEFAULT_LOW_BUFFER - AWAY_TEMP_SHIFT
        } else {
            DEFAULT_LOW_BOUND + DEFAULT_LOW_BUFFER
        }
        when {
            event.temperature.toFahrenheit().toFloat() <= onPoint -> {
                logger.debug("Temperature of <${device.name}> is below lower greenhouse bound. Currently ${event.temperature}ºF")
                heatingTimeouts.getOrPut(room) {
                    backgroundScope.launch {
                        logger.debug("Starting duty cycle loop")
                        while (true) {
                            switchHeat(room, SwitchState.ON)
                            delay(1.hours)
                            logger.info("${room.name} heat has been on for 1 hour. Turning off for 20 minutes.")
                            switchHeat(room, SwitchState.OFF)
                            delay(20.minutes)
                            logger.info("Resuming heat cycle.")
                        }
                    }
                }
            }
            event.temperature.toFahrenheit().toFloat() > offPoint -> {
                logger.trace("Temperature of <${device.name}> is within bounds <${event.temperature}ºF>")
                heatingTimeouts.remove(room)?.cancelAndJoin()
                switchHeat(room, SwitchState.OFF)
            }
            else -> {
                logger.trace("Temperature is within buffer range <${event.temperature}ºF>. Taking No action.")
            }
        }
    }

    private suspend fun switchHeat(room: Room, state: SwitchState) {
        room.devices
            .filter { it.fixture == Fixture.Heat }
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
