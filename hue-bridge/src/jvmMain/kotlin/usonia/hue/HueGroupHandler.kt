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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import usonia.foundation.*
import usonia.kotlin.*
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
        client.site.collectLatest { site ->
            client.actions
                .filter { it::class in HueArchetypes.group.actions }
                .map { action -> site.getDevice(action.target) to action }
                .filter { (device, _) -> site.findAssociatedBridge(device)?.service == HUE_SERVICE }
                .filter { (device, _) -> device.capabilities.archetypeId == HueArchetypes.group.archetypeId }
                .collectOn(requestScope) { (device, action) ->
                    handleAction(action, device)
                }
        }
    }

    private fun handleAction(action: Action, device: Device) {
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
