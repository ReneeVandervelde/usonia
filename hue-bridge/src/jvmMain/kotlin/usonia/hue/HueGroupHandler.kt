package usonia.hue

import com.github.ajalt.colormath.ConvertibleColor
import inkapplications.shade.constructs.Coordinates
import inkapplications.shade.constructs.asPercentage
import inkapplications.shade.constructs.mireds
import inkapplications.shade.groups.GroupStateModification
import inkapplications.shade.groups.ShadeGroups
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import usonia.core.Daemon
import usonia.foundation.Action
import usonia.foundation.SwitchState
import usonia.kotlin.neverEnding
import usonia.state.ActionAccess
import usonia.state.ConfigurationAccess
import usonia.state.getDeviceById

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
        actionAccess.actions
            .filter { it is Action.Switch || it is Action.Dim || it is Action.ColorTemperatureChange || it is Action.ColorChange }
            .map { configurationAccess.getDeviceById(it.target) to it }
            .filter { (device, _) -> device?.source?.service == "HueGroup" }
            .onEach { (device, _) -> logger.info("Handling Hue Action for ${device!!.name}") }
            .collect { (device, action) ->
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
                    else -> throw IllegalStateException()
                }

                shade.setState(device!!.source!!.id, modification)
            }
    }

}
