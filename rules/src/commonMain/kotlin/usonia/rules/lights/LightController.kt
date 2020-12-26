package usonia.rules.lights

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.selects.select
import usonia.core.Daemon
import usonia.core.state.ActionPublisher
import usonia.core.state.ConfigurationAccess
import usonia.core.state.EventAccess
import usonia.core.state.allAway
import usonia.foundation.*
import usonia.foundation.Room.Type.*
import usonia.kotlin.neverEnding
import kotlin.coroutines.CoroutineContext
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
    private val logger: KimchiLogger = EmptyLogger,
): Daemon, CoroutineScope {
    override val coroutineContext: CoroutineContext = Dispatchers.Default + SupervisorJob()

    override suspend fun start(): Nothing = neverEnding {
        configurationAccess.site.collectLatest { site ->
            eventAccess.events.filterIsInstance<Event.Motion>().collect { event ->
                onMotionEvent(event, site)
            }
        }
    }

    private suspend fun onMotionEvent(event: Event.Motion, site: Site) {
        if (eventAccess.allAway(site.users)) {
            logger.info("All ${site.users.size} users are away. Ignoring Motion Event.")
            return
        }

        val room = site.findRoomWithDevice(event.source)
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

    private suspend fun onRoomIdle(room: Room) = launch {
        val cancellation = launch {
            eventAccess.events
                .filterIsInstance<Event.Motion>()
                .filter { it.state == MotionState.MOTION }
                .filter { event ->
                    room.devices.any { it.id == event.source }
                }
                .first()
            logger.trace("Cancelling idle timer for ${room.name}")
        }
        val action = async {
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

        actions.forEach { actionPublisher.publishAction(it) }
    }

    private suspend fun onRoomMotion(room: Room) {
        logger.trace("Turning on lights in ${room.name}")
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

