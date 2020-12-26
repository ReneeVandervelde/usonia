package usonia.hue

import inkapplications.shade.constructs.Coordinates
import inkapplications.shade.constructs.asPercentage
import inkapplications.shade.constructs.mireds
import inkapplications.shade.groups.GroupStateModification
import inkapplications.shade.groups.ShadeGroups
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.flow.*
import usonia.core.Daemon
import usonia.core.state.ActionAccess
import usonia.core.state.ConfigurationAccess
import usonia.foundation.*
import usonia.kotlin.neverEnding

/**
 * Handles actions sent to Hue Group devices.
 */
internal class HueGroupHandler(
    private val actionAccess: ActionAccess,
    private val configurationAccess: ConfigurationAccess,
    private val shade: ShadeGroups,
    private val logger: KimchiLogger = EmptyLogger,
): Daemon {
    override suspend fun start() = neverEnding {
        configurationAccess.site.collectLatest { site -> onSiteUpdate(site) }
    }

    private suspend fun onSiteUpdate(site: Site) {
        actionAccess.actions
            .filter { it is Action.Switch || it is Action.Dim || it is Action.ColorTemperatureChange || it is Action.ColorChange }
            .collect { action ->
                handleAction(site, action)
            }
    }

    private suspend fun handleAction(site: Site, action: Action) {
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

        shade.setState(device.parent!!.id.value, modification)
    }
}
