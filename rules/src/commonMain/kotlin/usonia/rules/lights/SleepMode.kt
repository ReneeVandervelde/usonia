package usonia.rules.lights

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import usonia.core.state.*
import usonia.foundation.*
import usonia.kotlin.neverEnding
import usonia.kotlin.unit.percent
import usonia.rules.Flags
import usonia.rules.sleepMode
import usonia.server.Daemon
import usonia.server.client.BackendClient
import usonia.server.cron.CronJob
import usonia.server.cron.Schedule
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

private const val NIGHT_START = "sleep.night.start"
private const val DEFAULT_NIGHT = 20 * 60
private const val NIGHT_END = "sleep.night.end"
private const val DEFAULT_NIGHT_END = 4 * 60

/**
 * Keeps bedroom and adjacent rooms dim/off when sleep mode is enabled.
 */
@OptIn(ExperimentalTime::class)
internal class SleepMode(
    private val client: BackendClient,
    private val logger: KimchiLogger = EmptyLogger,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
): LightSettingsPicker, Daemon, CronJob {

    override val schedule: Schedule = Schedule(
        hours = setOf(9),
        minutes = setOf(0),
    )

    override suspend fun getRoomSettings(room: Room): LightSettings {
        if (!client.getBooleanFlag(Flags.SleepMode)) return LightSettings.Unhandled

        return when (room.type) {
            Room.Type.Bathroom -> LightSettings.Temperature(
                temperature = Colors.Warm,
                brightness = 5.percent,
            )
            Room.Type.Hallway -> {
                if (client.hasAdjacentType(room, Room.Type.Bedroom)) {
                    LightSettings.Temperature(
                        temperature = Colors.Warm,
                        brightness = 2.percent,
                    )
                } else {
                    LightSettings.Unhandled
                }
            }
            Room.Type.Bedroom -> LightSettings.Ignore
            else -> LightSettings.Unhandled
        }
    }

    override suspend fun run(time: LocalDateTime, timeZone: TimeZone) {
        client.setFlag(Flags.SleepMode, false)
    }

    override suspend fun start(): Nothing = neverEnding {
        coroutineScope {
            launch { autoEnable() }
            launch { lightsOffOnEnable() }
        }
    }

    private suspend fun lightsOffOnEnable() {
        client.site.collectLatest { site ->
            client.sleepMode
                .distinctUntilChanged()
                .filter { enabled -> enabled }
                .collect {
                    val bedrooms = site.rooms
                        .filter { it.type == Room.Type.Bedroom }

                    bedrooms.flatMap(::getDimActions).run { client.publishAll(this) }
                    delay(30.seconds)
                    bedrooms.flatMap(::getOffActions).run { client.publishAll(this) }
                }
        }
    }

    private fun getDimActions(room: Room): List<Action> {
        val colorTemperatureDevices = room.devices
            .filter { Fixture.Light == it.fixture }
            .filter { Action.ColorTemperatureChange::class in it.capabilities.actions }
            .map {
                Action.ColorTemperatureChange(
                    target = it.id,
                    temperature = Colors.Warm,
                    level = 2.percent,
                    switchState = SwitchState.ON,
                )
            }
        val dimmingDevices = room.devices
            .filter { Fixture.Light == it.fixture }
            .filter { Action.ColorTemperatureChange::class !in it.capabilities.actions }
            .filter { Action.Dim::class in it.capabilities.actions }
            .map {
                Action.Dim(
                    target = it.id,
                    level = 2.percent,
                    switchState = SwitchState.ON,
                )
            }

        return colorTemperatureDevices + dimmingDevices
    }

    private fun getOffActions(room: Room): List<Action> {
        return room.devices
            .filter { Fixture.Light == it.fixture }
            .map {
                Action.Switch(
                    target = it.id,
                    state = SwitchState.OFF,
                )
            }
    }

    private suspend fun autoEnable() {
        client.site.collectLatest { site ->
            val nightStartMinute = site.parameters[NIGHT_START]
                ?.toInt()
                ?: DEFAULT_NIGHT
            val nightEndMinute = site.parameters[NIGHT_END]
                ?.toInt()
                ?: DEFAULT_NIGHT_END

            client.events
                .filter { clock.currentMinuteOfDay >= nightStartMinute || clock.currentMinuteOfDay <= nightEndMinute }
                .filterIsInstance<Event.Latch>()
                .filter { it.state == LatchState.CLOSED }
                .filter { site.getRoomContainingDevice(it.source).type == Room.Type.Bedroom }
                .onEach { logger.info("Enabling Night Mode") }
                .collect { client.setFlag(Flags.SleepMode, true) }
        }
    }

    private val Clock.currentMinuteOfDay: Int get() = now().toLocalDateTime(timeZone).let { (it.hour * 60) + it.minute }
}
