package usonia.hue

import com.inkapplications.coroutines.ongoing.collectLatest
import com.inkapplications.coroutines.ongoing.filter
import com.inkapplications.coroutines.ongoing.flatMapLatest
import inkapplications.shade.events.Events
import inkapplications.shade.groupedlights.events.GroupedLightEvent
import inkapplications.shade.lights.events.LightEvent
import inkapplications.shade.structures.PowerInfo
import inkapplications.shade.structures.UndocumentedApi
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import regolith.processes.daemon.Daemon
import usonia.core.state.ConfigurationAccess
import usonia.core.state.EventPublisher
import usonia.foundation.*

@OptIn(UndocumentedApi::class)
class HueEventPublisher(
    private val events: Events,
    private val configurationAccess: ConfigurationAccess,
    private val eventPublisher: EventPublisher,
    private val clock: Clock = Clock.System,
    private val logger: KimchiLogger = EmptyLogger,
): Daemon {
    override suspend fun startDaemon(): Nothing {
        configurationAccess.site
            .filter { it.findBridgeByServiceTag(HUE_SERVICE) != null }
            .flatMapLatest { site ->
                events.bridgeEvents().map { it to site }
            }
            .collectLatest { (events, site) ->
                events.forEach { event ->
                    when (event) {
                        is LightEvent -> publishPowerEvent(site, event.powerInfo, Identifier(event.id.value))
                        is GroupedLightEvent -> publishPowerEvent(site, event.powerInfo, Identifier(event.id.value))
                        else -> logger.debug("Received unhandled hue event type: ${event::class.simpleName}")
                    }
                }
            }
    }

    private suspend fun publishPowerEvent(site: Site, powerInfo: PowerInfo?, externalId: Identifier) {
        val bridge = site.findBridgeByServiceTag(HUE_SERVICE) ?: run {
            logger.error("Hue bridge not configured while parsing event")
            return
        }
        val sourceDevice = site.findBridgeDevice(bridge.id, externalId) ?: run {
            logger.warn("No device associated with bridge: ${bridge.id} id: ${externalId.value}")
            return
        }

        powerInfo
            ?.let { power -> if (power.on) SwitchState.ON else SwitchState.OFF }
            ?.let {
                Event.Switch(
                    source = sourceDevice.id,
                    timestamp = clock.now(),
                    state = it,
                )
            }
            ?.run { eventPublisher.publishEvent(this) }
    }
}
