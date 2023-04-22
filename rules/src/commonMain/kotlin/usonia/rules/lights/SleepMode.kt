package usonia.rules.lights

import inkapplications.spondee.scalar.percent
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import usonia.core.state.getBooleanFlag
import usonia.core.state.hasAdjacentType
import usonia.core.state.publishAll
import usonia.core.state.setFlag
import usonia.foundation.*
import usonia.kotlin.*
import usonia.kotlin.datetime.*
import usonia.rules.Flags
import usonia.rules.sleepMode
import usonia.server.Daemon
import usonia.server.client.BackendClient
import usonia.server.cron.CronJob
import usonia.server.cron.Schedule
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

private const val NIGHT_START = "sleep.night.start"
private const val DEFAULT_NIGHT = 20 * 60
private const val NIGHT_END = "sleep.night.end"
private const val DEFAULT_NIGHT_END = 4 * 60

/**
 * Keeps bedroom and adjacent rooms dim/off when sleep mode is enabled.
 */
internal class SleepMode(
    private val client: BackendClient,
    private val logger: KimchiLogger = EmptyLogger,
    private val clock: ZonedClock = ZonedSystemClock,
): LightSettingsPicker, Daemon, CronJob {

    override val schedule: Schedule = Schedule(
        hours = setOf(9),
        minutes = setOf(0),
    )

    override suspend fun getActiveSettings(room: Room): LightSettings {
        if (!client.getBooleanFlag(Flags.SleepMode)) return LightSettings.Unhandled

        return when (room.type) {
            Room.Type.Bathroom -> {
                if (clock.current.localDateTime.minuteOfDay > 4 * 60 && clock.current.localDateTime.minuteOfDay < 12 * 60) {
                    LightSettings.Unhandled
                } else {
                    LightSettings.Temperature(
                        temperature = Colors.Warm,
                        brightness = 5.percent,
                    )
                }
            }
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

    override suspend fun runCron(time: ZonedDateTime) {
        logger.info("Auto-Disabling Sleep Mode.")
        client.setFlag(Flags.SleepMode, false)
    }

    override suspend fun start(): Nothing {
        client.site.collectLatest { site ->
            coroutineScope {
                launch { autoEnable(site) }
                launch { lightsOffOnEnable(site) }
                launch { intentEnable(site) }
            }
        }
    }

    private suspend fun lightsOffOnEnable(site: Site) {
        client.sleepMode
            .distinctUntilChanged()
            .filterTrue()
            .collectLatest {
                logger.info("Adjusting Lights for Sleep Mode.")
                val bedrooms = site.rooms.filter { it.type == Room.Type.Bedroom }

                bedrooms.flatMap(::getDimActions).run { client.publishAll(this) }
                delay(30.seconds)
                bedrooms.flatMap(::getOffActions).run { client.publishAll(this) }
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

    private suspend fun intentEnable(site: Site) {
        val nightStartMinute = site.parameters[NIGHT_START]
            ?.toInt()
            ?: DEFAULT_NIGHT
        val nightEndMinute = site.parameters[NIGHT_END]
            ?.toInt()
            ?: DEFAULT_NIGHT_END

        client.actions.filterIsInstance<Action.Intent>()
            .filter { it.action == "bed.enter" }
            .filter { clock.current.localDateTime.minuteOfDay >= nightStartMinute || clock.current.localDateTime.minuteOfDay <= nightEndMinute }
            .onEach { logger.info("Enabling Night Mode based on Intent") }
            .collectLatest { client.setFlag(Flags.SleepMode, true) }
    }

    private suspend fun autoEnable(site: Site) {
        val nightStartMinute = site.parameters[NIGHT_START]
            ?.toInt()
            ?: DEFAULT_NIGHT
        val nightEndMinute = site.parameters[NIGHT_END]
            ?.toInt()
            ?: DEFAULT_NIGHT_END

        client.events
            .filter { clock.current.localDateTime.minuteOfDay >= nightStartMinute || clock.current.localDateTime.minuteOfDay <= nightEndMinute }
            .filterIsInstance<Event.Latch>()
            .filter { it.state == LatchState.CLOSED }
            .filter { site.findRoomContainingDevice(it.source)?.type == Room.Type.Bedroom }
            .onEach { logger.info("Enabling Night Mode based on door latch") }
            .collectLatest { client.setFlag(Flags.SleepMode, true) }
    }
}
