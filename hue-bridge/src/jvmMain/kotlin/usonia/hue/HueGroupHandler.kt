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
import usonia.foundation.Action
import usonia.foundation.Bridge
import usonia.foundation.Site
import usonia.foundation.SwitchState
import usonia.kotlin.neverEnding
import usonia.state.ActionAccess
import usonia.state.ConfigurationAccess

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
        configurationAccess.site.collectLatest { site ->
            actionAccess.actions
                .filter { it is Action.Switch || it is Action.Dim || it is Action.ColorTemperatureChange || it is Action.ColorChange }
                .collect { action ->
                    handleAction(action, site)
                }
        }
    }

    private suspend fun handleAction(action: Action, site: Site) {
        val bridge = site.bridges
            .filterIsInstance<Bridge.Hue>()
            .singleOrNull()
            ?: run {
                logger.debug("Hud bridge not configured. Not attempting to handle Action for <${action.target}>")
                return
            }

        val hueId = bridge.deviceMap[action.target]

        if (hueId == null) {
            logger.trace("Not handling action for <${action.target}> – not associated with Hue Bridge.")
            return
        }

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

        shade.setState(hueId, modification)
    }
}
