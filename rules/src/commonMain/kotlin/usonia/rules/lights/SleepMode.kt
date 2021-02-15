package usonia.rules.lights

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import usonia.core.state.getBooleanFlag
import usonia.core.state.hasAdjacentType
import usonia.core.state.setFlag
import usonia.foundation.Event
import usonia.foundation.LatchState
import usonia.foundation.Room
import usonia.foundation.getRoomContainingDevice
import usonia.kotlin.neverEnding
import usonia.kotlin.unit.percent
import usonia.server.Daemon
import usonia.server.client.BackendClient

private const val FLAG = "Sleep Mode"
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
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
): LightSettingsPicker, Daemon {
    override suspend fun getRoomSettings(room: Room): LightSettings {
        if (!client.getBooleanFlag(FLAG)) return LightSettings.Unhandled

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

    override suspend fun start(): Nothing = neverEnding {
        coroutineScope {
            launch { autoEnable() }
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
                .collect { client.setFlag(FLAG, true) }
        }
    }

    private val Clock.currentMinuteOfDay: Int get() = now().toLocalDateTime(timeZone).let { (it.hour * 60) + it.minute }
}
