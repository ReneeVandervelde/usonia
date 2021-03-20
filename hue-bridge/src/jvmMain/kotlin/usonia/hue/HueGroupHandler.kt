package usonia.hue

import inkapplications.shade.constructs.Coordinates
import inkapplications.shade.constructs.asPercentage
import inkapplications.shade.constructs.mireds
import inkapplications.shade.groups.GroupStateModification
import inkapplications.shade.groups.ShadeGroups
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import usonia.foundation.*
import usonia.kotlin.IoScope
import usonia.kotlin.collectLatest
import usonia.server.Daemon
import usonia.server.client.BackendClient
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

/**
 * Handles actions sent to Hue Group devices.
 */
@OptIn(ExperimentalTime::class)
internal class HueGroupHandler(
    private val client: BackendClient,
    private val shade: ShadeGroups,
    private val logger: KimchiLogger = EmptyLogger,
    private val requestScope: CoroutineScope = IoScope()
): Daemon {
    override suspend fun start(): Nothing {
        client.site.collectLatest { site -> onSiteUpdate(site) }
    }

    private suspend fun onSiteUpdate(site: Site) {
        client.actions
            .filter { it is Action.Switch || it is Action.Dim || it is Action.ColorTemperatureChange || it is Action.ColorChange }
            .collect { action ->
                handleAction(site, action)
            }
    }

    private fun handleAction(site: Site, action: Action) {
        val device = site.getDevice(action.target)
        val bridge = site.findAssociatedBridge(device)
        if (bridge?.service != HUE_SERVICE) {
            logger.trace("Ignoring non-hue action <$action>")
            return
        }
        logger.trace { "Handling ${action::class.simpleName} for ${device.name}" }

        val modification = when (action) {
            is Action.Switch -> GroupStateModification(
                on = action.state == SwitchState.ON,
            )
            is Action.Dim -> GroupStateModification(
                brightness = action.level.fraction.asPercentage,
                on = action.switchState?.equals(SwitchState.ON),
            )
            is Action.ColorTemperatureChange -> GroupStateModification(
                colorTemperature = action.temperature.miredValue.mireds,
                brightness = action.level?.fraction?.asPercentage,
                on = action.switchState?.equals(SwitchState.ON),
            )
            is Action.ColorChange -> GroupStateModification(
                cieColorCoordinates = action.color.let { Coordinates(it) },
                brightness = action.level?.fraction?.asPercentage,
                on = action.switchState?.equals(SwitchState.ON),
            )
            else -> throw IllegalStateException("Impossible! Did the event filtering change without updating the modification conditions?")
        }

        requestScope.launch {
            try {
                withTimeout(5.seconds) {
                    shade.setState(device.parent!!.id.value, modification)
                }
            } catch (e: CancellationException) {
                logger.warn("Hue Action was Cancelled", e)
                throw e
            } catch (e: Throwable) {
                logger.error("Error setting hue group state.", e)
            }
        }
    }
}
