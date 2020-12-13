package usonia.rules.lights

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import usonia.core.Daemon
import usonia.core.state.ActionPublisher
import usonia.core.state.ConfigurationAccess
import usonia.core.state.EventAccess
import usonia.foundation.*
import usonia.foundation.Room.Type.*
import usonia.kotlin.neverEnding
import kotlin.time.ExperimentalTime
import kotlin.time.minutes
import kotlin.time.seconds

/**
 * Controls the on/off state of a room's lights.
 */
@OptIn(ExperimentalTime::class)
internal class LightController(
    private val configurationAccess: ConfigurationAccess,
    private val eventAccess: EventAccess,
    private val actionPublisher: ActionPublisher,
    private val colorPicker: ColorPicker,
): Daemon {
    override suspend fun start(): Nothing = neverEnding {
        configurationAccess.site.collectLatest { site ->
            eventAccess.events.filterIsInstance<Event.Motion>().collect { event ->
                val room = site.findRoomWithDevice(event.source)
                when (event.state) {
                    MotionState.MOTION -> onRoomMotion(room)
                    MotionState.IDLE -> onRoomIdle(room)
                }
            }
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
        coroutineScope {
            val cancellation = launch {
                eventAccess.events
                    .filterIsInstance<Event.Motion>()
                    .filter { it.state == MotionState.MOTION }
                    .filter { event ->
                        room.devices.any { it.id == event.source }
                    }
                    .first()
            }
            val action = async { delay(room.idleTime).let { room } }
            val cancelled = select<Room?> {
                cancellation.onJoin { null }
                action.onAwait { it }
            }
            cancellation.cancel()
            action.cancel()

            if (cancelled != null) switchRoomOff(cancelled)
        }
    }

    private suspend fun switchRoomOff(room: Room) {
        val actions = room.devices
            .filter { Action.Switch::class in it.capabilities.actions }
            .map {
                Action.Switch(
                    target = it.id,
                    state = SwitchState.OFF,
                )
            }

        actions.forEach { actionPublisher.publishAction(it) }
    }

    private suspend fun onRoomMotion(room: Room) {
        val color = colorPicker.getRoomColor(room)
        val colorTemperatureDevices = room.devices
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
            .filter { Action.ColorTemperatureChange::class !in it.capabilities.actions }
            .filter { Action.Dim::class !in it.capabilities.actions }
            .filter { Action.Switch::class in it.capabilities.actions }
            .map {
                Action.Switch(
                    target = it.id,
                    state = SwitchState.ON,
                )
            }

        (colorTemperatureDevices + dimmingDevices + switchDevices).forEach {
            actionPublisher.publishAction(it)
        }
    }
}

