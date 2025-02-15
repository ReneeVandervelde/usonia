package usonia.rules.lights

import com.inkapplications.coroutines.ongoing.*
import inkapplications.spondee.scalar.percent
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import regolith.processes.daemon.Daemon
import usonia.core.state.getBooleanFlag
import usonia.core.state.getSite
import usonia.core.state.publishAll
import usonia.core.state.setFlag
import usonia.foundation.*
import usonia.foundation.Room.Type.*
import usonia.kotlin.DefaultScope
import usonia.rules.Flags
import usonia.server.client.BackendClient
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Dims lights during when a flag is set.
 */
internal class MovieMode(
    private val client: BackendClient,
    private val backgroundScope: CoroutineScope = DefaultScope(),
    private val logger: KimchiLogger = EmptyLogger,
): LightSettingsPicker, Daemon {
    override suspend fun getActiveSettings(room: Room): LightSettings {
        if (!client.getBooleanFlag(Flags.MovieMode)) {
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

    override suspend fun getIdleConditions(room: Room): IdleConditions {
        if (!client.getBooleanFlag(Flags.MovieMode)) {
            return IdleConditions.Unhandled
        }
        return when(room.type) {
            Hallway -> IdleConditions.Timed(5.seconds)
            Kitchen, Dining -> IdleConditions.Timed(30.seconds)
            Bathroom -> IdleConditions.Timed(2.minutes)
            else -> IdleConditions.Unhandled
        }
    }

    override suspend fun startDaemon(): Nothing {
        client.flags
            .map { it[Flags.MovieMode].toBoolean() }
            .drop(1)
            .distinctUntilChanged()
            .collectLatest { enabled ->
                if (enabled) startMovieMode() else stopMovieMode()
            }
    }

    private suspend fun startMovieMode() {
        logger.info("Adjusting Lights for Movie Mode.")
        val switchActions = client.getSite().rooms
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
        val indicatorActions = client.getSite().indicators
            .map {
                Action.Dim(
                    target = it.id,
                    level = 1.percent,
                )
            }

        client.publishAll(switchActions + indicatorActions)

        val endObserver = backgroundScope.launch {
            client.flags
                .map { it[Flags.MovieMode].toBoolean() }
                .filter { !it }
                .first()
        }

        val timer = backgroundScope.launch {
            delay(3.hours)
        }

        val result = select {
            endObserver.onJoin { EndCondition.Disabled }
            timer.onJoin { EndCondition.Expired }
        }
        endObserver.cancelAndJoin()
        timer.cancelAndJoin()

        when (result) {
            EndCondition.Disabled -> {
                logger.info("Movie Mode was disabled. Ending timer")
            }
            EndCondition.Expired -> {
                logger.info("Movie Mode timer expired. Ending Movie Mode.")
                client.setFlag(Flags.MovieMode, false)
            }
        }
    }

    private enum class EndCondition {
        Disabled,
        Expired,
    }

    private suspend fun stopMovieMode() {
        logger.info("Adjusting Lights to exit Movie Mode.")
        val lightActions = client.getSite().rooms
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
        val indicatorActions = client.getSite().indicators
            .map {
                Action.Dim(
                    target = it.id,
                    level = 50.percent,
                )
            }
        client.publishAll(lightActions + indicatorActions)
    }

    private val Site.indicators get() = rooms
        .filter { it.type == LivingRoom }
        .flatMap { it.devices }
        .filter { it.fixture == Fixture.Indicator }
        .filter { Action.Dim::class in it.capabilities.actions }
}
