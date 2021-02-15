package usonia.rules.lights

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.selects.select
import usonia.core.state.publishAll
import usonia.foundation.*
import usonia.foundation.Room.Type.*
import usonia.kotlin.DefaultScope
import usonia.kotlin.neverEnding
import usonia.server.Daemon
import usonia.server.client.BackendClient
import kotlin.time.ExperimentalTime
import kotlin.time.minutes
import kotlin.time.seconds

/**
 * Controls the on/off state of a room's lights.
 */
@OptIn(ExperimentalTime::class)
internal class LightController(
    private val client: BackendClient,
    private val lightSettingsPicker: LightSettingsPicker,
    private val logger: KimchiLogger = EmptyLogger,
    private val backgroundScope: CoroutineScope = DefaultScope(),
): Daemon {

    override suspend fun start(): Nothing = neverEnding {
        client.site.collectLatest { site ->
            client.events.filterIsInstance<Event.Motion>().collect { event ->
                backgroundScope.launch { onMotionEvent(event, site) }
            }
        }
    }

    private suspend fun onMotionEvent(event: Event.Motion, site: Site) {
        val room = site.getRoomContainingDevice(event.source)
        when (event.state) {
            MotionState.MOTION -> onRoomMotion(room)
            MotionState.IDLE -> onRoomIdle(room)
        }
    }

    private val Room.idleTime get() = when(type) {
        Bathroom -> 5.minutes
        Bedroom -> 5.minutes
        Dining -> 10.minutes
        Garage -> 15.minutes
        Generic -> 10.minutes
        Hallway -> 5.seconds
        Kitchen -> 10.minutes
        LivingRoom -> 30.minutes
        Office -> 15.minutes
        Storage -> 1.minutes
        Utility -> 1.minutes
    }

    private suspend fun onRoomIdle(room: Room) {
        val cancellation = backgroundScope.launch {
            client.events
                .filterIsInstance<Event.Motion>()
                .filter { it.state == MotionState.MOTION }
                .filter { it.source in room }
                .first()
            logger.trace("Cancelling idle timer for ${room.name}")
        }
        val action = backgroundScope.async {
            logger.trace("Starting ${room.idleTime} idle timer for ${room.name}")
            delay(room.idleTime).let { room }
        }
        val cancelled = select<Room?> {
            cancellation.onJoin { null }
            action.onAwait { it }
        }
        cancellation.cancel()
        action.cancel()

        if (cancelled != null) switchRoomOff(cancelled)
    }

    private suspend fun switchRoomOff(room: Room) {
        logger.trace("Switching lights off in ${room.name}")
        val actions = room.devices
            .filter { Action.Switch::class in it.capabilities.actions }
            .map {
                Action.Switch(
                    target = it.id,
                    state = SwitchState.OFF,
                )
            }

        client.publishAll(actions)
    }

    private suspend fun onRoomMotion(room: Room) {
        logger.trace("Adjusting lights in ${room.name}")
        val settings = lightSettingsPicker.getRoomSettings(room)

        when (settings) {
            is LightSettings.Temperature -> setRoomTemperature(room, settings)
            LightSettings.Ignore -> {
                logger.trace("Ignored action for room event: $room")
            }
            LightSettings.Unhandled -> {
                logger.warn("Unhandled action for room event: $room")
            }
        }
    }

    private suspend fun setRoomTemperature(room: Room, color: LightSettings.Temperature) {
        val colorTemperatureDevices = room.devices
            .filter { Fixture.Light == it.fixture }
            .filter { Action.ColorTemperatureChange::class in it.capabilities.actions }
            .map {
                Action.ColorTemperatureChange(
                    target = it.id,
                    temperature = color.temperature,
                    level = color.brightness,
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
                    level = color.brightness,
                    switchState = SwitchState.ON,
                )
            }
        val switchDevices = room.devices
            .filter { Fixture.Light == it.fixture }
            .filter { Action.ColorTemperatureChange::class !in it.capabilities.actions }
            .filter { Action.Dim::class !in it.capabilities.actions }
            .filter { Action.Switch::class in it.capabilities.actions }
            .map {
                Action.Switch(
                    target = it.id,
                    state = SwitchState.ON,
                )
            }

        client.publishAll(colorTemperatureDevices + dimmingDevices + switchDevices)
    }
}

