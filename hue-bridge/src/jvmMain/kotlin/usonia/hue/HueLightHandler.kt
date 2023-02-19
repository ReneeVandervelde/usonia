package usonia.hue

import inkapplications.shade.lights.LightControls
import inkapplications.shade.lights.parameters.ColorParameters
import inkapplications.shade.lights.parameters.ColorTemperatureParameters
import inkapplications.shade.lights.parameters.DimmingParameters
import inkapplications.shade.lights.parameters.LightUpdateParameters
import inkapplications.shade.structures.ApiStatusError
import inkapplications.shade.structures.ResourceId
import inkapplications.shade.structures.parameters.PowerParameters
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
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

/**
 * Handles actions sent to Hue Group devices.
 */
@OptIn(ExperimentalTime::class)
internal class HueLightHandler(
    private val client: BackendClient,
    private val shade: LightControls,
    private val logger: KimchiLogger = EmptyLogger,
    private val requestScope: CoroutineScope = IoScope()
): Daemon {
    override suspend fun start(): Nothing {
        client.site.collectLatest { site ->
            client.actions
                .filter { it::class in HueArchetypes.color.actions }
                .map { action -> site.getDevice(action.target) to action }
                .filter { (device, _) -> site.findAssociatedBridge(device)?.service == HUE_SERVICE }
                .filter { (device, _) -> device.capabilities.archetypeId == HueArchetypes.color.archetypeId }
                .collectOn(requestScope) { (device, action) ->
                    handleAction(action, device)
                }
        }
    }

    private fun handleAction(action: Action, device: Device) {
        logger.trace { "Handling ${action::class.simpleName} for ${device.name}" }

        val modification = when (action) {
            is Action.Switch -> LightUpdateParameters(
                power = PowerParameters(
                    on = action.state == SwitchState.ON,
                ),
            )
            is Action.Dim -> LightUpdateParameters(
                dimming = DimmingParameters(
                    brightness = action.level,
                ),
                power = action.switchState?.equals(SwitchState.ON)?.let {
                    PowerParameters(
                        on = it,
                    )
                }
            )
            is Action.ColorTemperatureChange -> LightUpdateParameters(
                colorTemperature = ColorTemperatureParameters(
                    temperature = action.temperature,
                ),
                dimming = action.level?.let {
                    DimmingParameters(brightness = it)
                },
                power = action.switchState?.equals(SwitchState.ON)?.let {
                    PowerParameters(
                        on = it,
                    )
                },
            )
            is Action.ColorChange -> LightUpdateParameters(
                color = ColorParameters(
                    color = action.color,
                ),
                dimming = action.level?.let {
                    DimmingParameters(brightness = it)
                },
                power = action.switchState?.equals(SwitchState.ON)?.let {
                    PowerParameters(
                        on = it,
                    )
                },
            )
            else -> throw IllegalStateException("Impossible! Did the event filtering change without updating the modification conditions?")
        }

        requestScope.launch {
            try {
                withTimeout(5.seconds) {
                    shade.updateLight(device.parent!!.id.value.let(::ResourceId), modification)
                }
            } catch (e: CancellationException) {
                logger.warn("Hue Action was Cancelled while updating light: ${device.id}", e)
                throw e
            } catch (e: ApiStatusError) {
                if (e.code == 207) {
                    logger.warn("Partial success from hue group update for light: ${device.name}", e)
                } else {
                    logger.error("API Error updating hue light: ${device.name}", e)
                }
            } catch (e: Throwable) {
                logger.error("Unknown error updating hue light: ${device.name}", e)
            }
        }
    }
}
