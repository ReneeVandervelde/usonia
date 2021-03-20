package usonia.rules.lights

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import usonia.core.state.getBooleanFlag
import usonia.core.state.getSite
import usonia.core.state.publishAll
import usonia.foundation.Action
import usonia.foundation.Fixture
import usonia.foundation.Room
import usonia.foundation.Room.Type.*
import usonia.foundation.SwitchState
import usonia.kotlin.collectLatest
import usonia.kotlin.drop
import usonia.kotlin.map
import usonia.kotlin.unit.percent
import usonia.server.Daemon
import usonia.server.client.BackendClient

private const val FLAG = "Movie Mode"

/**
 * Dims lights during when a flag is set.
 */
internal class MovieMode(
    private val client: BackendClient,
    private val logger: KimchiLogger = EmptyLogger,
): LightSettingsPicker, Daemon {
    override suspend fun getActiveSettings(room: Room): LightSettings {
        if (!client.getBooleanFlag(FLAG)) {
            return LightSettings.Unhandled
        }
        return when(room.type) {
            LivingRoom -> LightSettings.Ignore
            Kitchen, Hallway, Dining -> LightSettings.Temperature(
                temperature = Colors.Warm,
                brightness = 1.percent,
            )
            Bathroom -> LightSettings.Temperature(
                temperature = Colors.Warm,
                brightness = 50.percent,
            )
            Bedroom, Garage, Generic, Office, Storage, Utility, Greenhouse -> LightSettings.Unhandled
        }
    }

    override suspend fun start(): Nothing {
        client.flags
            .map { it[FLAG].toBoolean() }
            .drop(1)
            .collectLatest { enabled ->
                if (enabled) startMovieMode() else stopMovieMode()
            }
    }

    private suspend fun startMovieMode() {
        logger.info("Adjusting Lights for Movie Mode.")
        client.getSite().rooms
            .filter { it.type in setOf(LivingRoom, Kitchen, Hallway, Dining) }
            .flatMap { it.devices }
            .filter { it.fixture == Fixture.Light }
            .filter { Action.Switch::class in it.capabilities.actions }
            .map {
                Action.Switch(
                    target = it.id,
                    state = SwitchState.OFF,
                )
            }
            .run { client.publishAll(this) }
    }

    private suspend fun stopMovieMode() {
        logger.info("Adjusting Lights to exit Movie Mode.")
        client.getSite().rooms
            .filter { it.type == LivingRoom }
            .flatMap { it.devices }
            .filter { it.fixture == Fixture.Light }
            .filter { Action.Switch::class in it.capabilities.actions }
            .map {
                Action.ColorTemperatureChange(
                    target = it.id,
                    temperature = Colors.Warm,
                    level = 10.percent,
                    switchState = SwitchState.ON,
                )
            }
            .run { client.publishAll(this) }
    }
}
