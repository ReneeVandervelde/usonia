package usonia.rules.lights

import com.inkapplications.coroutines.ongoing.*
import com.inkapplications.datetime.ZonedClock
import inkapplications.spondee.scalar.percent
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import regolith.processes.cron.CronJob
import regolith.processes.cron.Schedule
import regolith.processes.daemon.Daemon
import usonia.core.state.getBooleanFlag
import usonia.core.state.hasAdjacentType
import usonia.core.state.publishAll
import usonia.core.state.setFlag
import usonia.foundation.*
import usonia.kotlin.DefaultScope
import usonia.kotlin.datetime.minuteOfDay
import usonia.kotlin.filterTrue
import usonia.rules.Flags
import usonia.rules.sleepMode
import usonia.server.client.BackendClient
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val NIGHT_START = "sleep.night.start"
private val DEFAULT_NIGHT = 20.hours.inWholeMinutes
private const val NIGHT_END = "sleep.night.end"
private val DEFAULT_NIGHT_END = 4.hours.inWholeMinutes

private const val MORNING_START = "sleep.morning.start"
private val DEFAULT_MORNING_START = 5.hours.inWholeMinutes
private const val MORNING_END = "sleep.morning.end"
private val DEFAULT_MORNING_END = 16.hours.inWholeMinutes

/**
 * Keeps bedroom and adjacent rooms dim/off when sleep mode is enabled.
 */
internal class SleepMode(
    private val client: BackendClient,
    private val logger: KimchiLogger = EmptyLogger,
    private val clock: ZonedClock = ZonedClock.System,
    private val backgroundScope: CoroutineScope = DefaultScope(),
): LightSettingsPicker, Daemon, CronJob {

    override val schedule: Schedule = Schedule(
        hours = setOf(12),
        minutes = setOf(0),
    )

    override suspend fun getActiveSettings(room: Room): LightSettings {
        if (!client.getBooleanFlag(Flags.SleepMode)) return LightSettings.Unhandled

        return when (room.type) {
            Room.Type.Bathroom -> {
                if (clock.localDateTime().minuteOfDay > 4 * 60 && clock.localDateTime().minuteOfDay < 12 * 60) {
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

    override suspend fun runCron(time: LocalDateTime, zone: TimeZone) {
        logger.info("Auto-Disabling Sleep Mode by cron.")
        client.setFlag(Flags.SleepMode, false)
    }

    override suspend fun startDaemon(): Nothing {
        client.site.collectLatest { site ->
            coroutineScope {
                launch { autoEnable(site) }
                launch { autoDisable(site) }
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
                delay(90.seconds)
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
            ?.toLong()
            ?: DEFAULT_NIGHT
        val nightEndMinute = site.parameters[NIGHT_END]
            ?.toLong()
            ?: DEFAULT_NIGHT_END

        client.actions.filterIsInstance<Action.Intent>()
            .filter { it.action == "bed.enter" }
            .filter { clock.localDateTime().minuteOfDay >= nightStartMinute || clock.localDateTime().minuteOfDay <= nightEndMinute }
            .onEach { logger.info("Enabling Night Mode based on Intent") }
            .collectLatest {
                backgroundScope.launch { client.setFlag(Flags.SleepMode, true) }
            }
    }

    private suspend fun autoEnable(site: Site) {
        val nightStartMinute = site.parameters[NIGHT_START]
            ?.toLong()
            ?: DEFAULT_NIGHT
        val nightEndMinute = site.parameters[NIGHT_END]
            ?.toLong()
            ?: DEFAULT_NIGHT_END

        client.events
            .filter { clock.localDateTime().minuteOfDay >= nightStartMinute || clock.localDateTime().minuteOfDay <= nightEndMinute }
            .filterIsInstance<Event.Latch>()
            .filter { it.state == LatchState.CLOSED }
            .filter { site.findRoomContainingDevice(it.source)?.type == Room.Type.Bedroom }
            .onEach { logger.info("Enabling Night Mode based on door latch") }
            .collectLatest { client.setFlag(Flags.SleepMode, true) }
    }

    private suspend fun autoDisable(site: Site) {
        return // temporarily disable this
        val morningStartMinute = site.parameters[MORNING_START]
            ?.toLong()
            ?: DEFAULT_MORNING_START
        val morningEndMinute = site.parameters[MORNING_END]
            ?.toLong()
            ?: DEFAULT_MORNING_END

        client.events
            .filter { clock.localDateTime().minuteOfDay in morningStartMinute..morningEndMinute }
            .filterIsInstance<Event.Latch>()
            .filter { it.state == LatchState.OPEN }
            .filter { site.findRoomContainingDevice(it.source)?.type == Room.Type.Bedroom }
            .onEach { logger.info("Launching wake disable timer") }
            .collectLatest { openEvent ->
                val cancellation = backgroundScope.launch {
                    client.events
                        .filterIsInstance<Event.Latch>()
                        .filter { it.state == LatchState.CLOSED }
                        .filter { it.source == openEvent.source }
                        .onEach { logger.info("Cancelling wake disable timer, door closed was closed.") }
                        .first()
                }
                val timer = backgroundScope.launch {
                    logger.debug("Starting timer for wake disable")
                    delay(10.minutes)
                    logger.debug("Wake disable timer complete")
                }
                val cancelled = select {
                    cancellation.onJoin { true }
                    timer.onJoin { false }
                }

                cancellation.cancel()
                timer.cancel()

                if (!cancelled) {
                    logger.info("Auto-Disabling Sleep Mode.")
                    client.setFlag(Flags.SleepMode, false)
                } else {
                    logger.info("Cancelling auto-wake action")
                }
            }
    }
}
