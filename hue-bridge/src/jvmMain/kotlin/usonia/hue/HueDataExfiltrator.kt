package usonia.hue

import inkapplications.shade.lights.LightControls
import inkapplications.shade.lights.structures.Light
import inkapplications.shade.structures.ResourceId
import inkapplications.spondee.scalar.toWholePercentage
import inkapplications.spondee.structure.roundToInt
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.datetime.Clock
import regolith.processes.daemon.Daemon
import usonia.core.client.getUserPresence
import usonia.core.state.findDevicesBy
import usonia.core.state.getSite
import usonia.core.state.publishAll
import usonia.foundation.Event
import usonia.foundation.Fixture
import usonia.foundation.PresenceState
import usonia.kotlin.OngoingFlow
import usonia.kotlin.asOngoing
import usonia.kotlin.collect
import usonia.server.client.BackendClient
import kotlin.time.Duration.Companion.minutes

/**
 * Extracts and publishes data passed from other APIs in the form of a hue
 * light setting.
 */
internal class HueDataExfiltrator(
    private val client: BackendClient,
    private val shade: LightControls,
    private val logger: KimchiLogger,
    private val clock: Clock = Clock.System,
): Daemon {
    private val polledLightEvents: OngoingFlow<Light> = flow {
        while (currentCoroutineContext().isActive) {
            val id = client.findDevicesBy { it.fixture == Fixture.DataExfiltrator }
                .firstOrNull()
                ?.parent?.id
            runCatching { id?.value?.let(::ResourceId)?.let { shade.getLight(it) } }
                .onSuccess { it?.let { emit(it) } }
                .onFailure { logger.error("Failed to poll light", it) }
            delay(5.minutes)
        }
    }.asOngoing()

    override suspend fun startDaemon(): Nothing {
        polledLightEvents.collect { light ->
            val data = extractLightData(light) ?: return@collect

            if (data.away) {
                client.getSite().users
                    .filter { client.getUserPresence(it.id) != PresenceState.AWAY }
                    .map { Event.Presence(it.id, clock.now(), PresenceState.AWAY) }
                    .run { client.publishAll(this) }
            } else {
                client.getSite().users
                    .filter { client.getUserPresence(it.id) != PresenceState.HOME }
                    .map { Event.Presence(it.id, clock.now(), PresenceState.HOME) }
                    .run { client.publishAll(this) }
            }
        }
    }

    private fun extractLightData(light: Light): ExternalData? {
        val brightness = light.dimmingInfo?.brightness?.toWholePercentage()?.roundToInt() ?: return null.also {
            logger.warn("Exfiltration Light has no brightness value")
        }

        return ExternalData(
            away = brightness and Signal.AWAY == Signal.AWAY,
        )
    }

    private object Signal {
        val AWAY = 0b000010
    }
}
