package usonia.rules.lights

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import regolith.processes.daemon.Daemon
import usonia.core.state.publishAll
import usonia.foundation.*
import usonia.kotlin.*
import usonia.server.client.BackendClient

/**
 * Controls the on/off state of a room's lights.
 */
internal class LightController(
    private val client: BackendClient,
    private val lightSettingsPicker: LightSettingsPicker,
    private val logger: KimchiLogger = EmptyLogger,
    private val backgroundScope: CoroutineScope = DefaultScope(),
): Daemon {
    override suspend fun startDaemon(): Nothing {
        client.site.collectLatest { site ->
            client.events
                .filterIsInstance<Event.Motion>()
                .collectOn(backgroundScope) { event -> onMotionEvent(event, site) }
        }
    }

    private suspend fun onMotionEvent(event: Event.Motion, site: Site) {
        val room = site.findRoomContainingDevice(event.source) ?: run {
            logger.error("Unable to find a room containing ${event.source}")
            return
        }
        when (event.state) {
            MotionState.MOTION -> onRoomMotion(room)
            MotionState.IDLE -> onRoomIdle(room)
        }
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
            val idleConditions = lightSettingsPicker.getIdleConditions(room)
            when (idleConditions) {
                is IdleConditions.Timed -> {
                    logger.trace("Starting ${idleConditions.time} idle timer for ${room.name}")
                    delay(idleConditions.time).let { room }
                }
                IdleConditions.Ignored -> {
                    logger.trace("Skipping Idle monitor for room $room")
                }
                IdleConditions.Unhandled -> {
                    logger.warn("Unhandled idle action for room: $room")
                }
            }
        }
        val cancelled = select<Room?> {
            cancellation.onJoin { null }
            action.onAwait { room }
        }
        cancellation.cancel()
        action.cancel()

        if (cancelled != null) {
            val settings = lightSettingsPicker.getIdleSettings(room)
            adjustRoomLights(cancelled, settings)
        }
    }

    private suspend fun onRoomMotion(room: Room) {
        logger.trace("Handling lights in ${room.name}")
        val settings = lightSettingsPicker.getActiveSettings(room)
        adjustRoomLights(room, settings)
    }
    
    private suspend fun adjustRoomLights(room: Room, settings: LightSettings) {
        logger.trace("Adjusting lights in ${room.name}")
        when (settings) {
            is LightSettings.Temperature -> setRoomTemperature(room, settings)
            is LightSettings.Switch -> switchRoomLights(room, settings.state)
            LightSettings.Ignore -> {
                logger.trace("Ignored action for room event: ${room.name}")
            }
            LightSettings.Unhandled -> {
                logger.warn("Unhandled action for room event: ${room.name}")
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

    private suspend fun switchRoomLights(room: Room, state: SwitchState) {
        logger.trace("Switching lights ${state.name} in ${room.name}")
        val actions = room.devices
            .filter { Fixture.Light == it.fixture }
            .filter { Action.Switch::class in it.capabilities.actions }
            .map {
                Action.Switch(
                    target = it.id,
                    state = state,
                )
            }

        client.publishAll(actions)
    }
}

