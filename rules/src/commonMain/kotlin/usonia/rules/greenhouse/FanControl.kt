package usonia.rules.greenhouse

import com.inkapplications.coroutines.ongoing.collect
import com.inkapplications.coroutines.ongoing.collectLatest
import com.inkapplications.coroutines.ongoing.filter
import com.inkapplications.coroutines.ongoing.filterIsInstance
import inkapplications.spondee.measure.us.toFahrenheit
import inkapplications.spondee.structure.toFloat
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import regolith.processes.cron.CronJob
import regolith.processes.cron.Schedule
import regolith.processes.daemon.Daemon
import regolith.processes.daemon.DaemonFailureHandler
import regolith.processes.daemon.DaemonRunAttempt
import regolith.processes.daemon.FailureSignal
import usonia.core.state.getSite
import usonia.core.state.publishAll
import usonia.core.state.rooms
import usonia.foundation.*
import usonia.kotlin.DefaultScope
import usonia.server.client.BackendClient
import kotlin.time.Duration.Companion.minutes

private const val DEFAULT_HIGH_BOUND = 80
private const val DEFAULT_UPPER_BUFFER = 3

/**
 * Turns fans on/off based on an upper-limit temperature.
 */
class FanControl(
    private val client: BackendClient,
    private val failureHandler: DaemonFailureHandler,
    private val logger: KimchiLogger = EmptyLogger,
    private val backgroundScope: CoroutineScope = DefaultScope(),
): Daemon, CronJob {
    private val cooling = MutableStateFlow<Set<Room>>(setOf())

    override val schedule: Schedule = Schedule()
        .withHours { it % 2 == 0 }
        .withMinutes { it == 0 }

    override suspend fun runCron(time: LocalDateTime, zone: TimeZone) {
        logger.info("Running greenhouse fans periodic fan for 10 minutes")
        backgroundScope.launch {
            val rooms = client.getSite().rooms
                .filter { it.type == Room.Type.Greenhouse }

            rooms.forEach { room -> switchFans(room, SwitchState.ON) }
            delay(10.minutes)
            (rooms - cooling.value).forEach { room -> switchFans(room, SwitchState.OFF) }
        }
    }

    override suspend fun startDaemon(): Nothing {
        client.site.collectLatest { site ->
            client.events
                .filterIsInstance<Event.Temperature>()
                .filter { event -> site.findRoomContainingDevice(event.source)?.type == Room.Type.Greenhouse }
                .collect { event ->
                    backgroundScope.launch { onTemperature(site, event) }
                }
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
            event.temperature.toFahrenheit().toFloat() >= DEFAULT_HIGH_BOUND -> {
                logger.info("Temperature of <${device.name}> exceeds upper greenhouse bound. Currently ${event.temperature}ºF")
                switchFans(room, SwitchState.ON)
                cooling.getAndUpdate {
                    it + room
                }
            }
            event.temperature.toFahrenheit().toFloat() < DEFAULT_HIGH_BOUND - DEFAULT_UPPER_BUFFER -> {
                logger.trace("Temperature of <${device.name}> is within bounds <${event.temperature}ºF>")
                switchFans(room, SwitchState.OFF)
                cooling.getAndUpdate { it - room }
            }
            else -> {
                logger.trace("Temperature is within buffer range <${event.temperature}ºF>. Taking No action.")
            }
        }
    }

    override suspend fun onFailure(attempts: List<DaemonRunAttempt>): FailureSignal {
        return failureHandler.onFailure(attempts)
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
