package usonia.hue

import com.inkapplications.standard.throwCancels
import inkapplications.shade.lights.LightControls
import inkapplications.shade.lights.parameters.*
import inkapplications.shade.structures.ApiStatusError
import inkapplications.shade.structures.ResourceId
import inkapplications.shade.structures.parameters.PowerParameters
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import regolith.processes.daemon.Daemon
import usonia.foundation.*
import usonia.kotlin.*
import usonia.server.client.BackendClient
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Handles actions sent to Hue Group devices.
 */
internal class HueLightHandler(
    private val client: BackendClient,
    private val shade: LightControls,
    private val logger: KimchiLogger = EmptyLogger,
    private val requestScope: CoroutineScope = IoScope()
): Daemon {
    override suspend fun startDaemon(): Nothing {
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
                dynamics = dynamicsForState(action),
            )
            is Action.Dim -> LightUpdateParameters(
                dimming = DimmingParameters(
                    brightness = action.level,
                ),
                power = action.switchState?.equals(SwitchState.ON)?.let {
                    PowerParameters(
                        on = it,
                    )
                },
                dynamics = dynamicsForState(action),
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
                dynamics = dynamicsForState(action),
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
                dynamics = dynamicsForState(action),
            )
            else -> throw IllegalStateException("Impossible! Did the event filtering change without updating the modification conditions?")
        }

        requestScope.launch {
            val executionResult = runRetryable(
                attemptTimeout = 5.seconds,
                strategy = RetryStrategy.Bracket(
                    attempts = 5,
                    timeouts = listOf(100.milliseconds, 800.milliseconds, 2.seconds),
                ),
                onError = { onError(it, device) },
                retryFilter = { it !is CancellationException && !(it is ApiStatusError && it.code == 207) },
            ) {
                shade.updateLight(device.parent!!.id.value.let(::ResourceId), modification)
            }.throwCancels()

            executionResult.onSuccess { reference ->
                logger.trace("Successfully updated Hue Group: ${device.name}")
            }.onFailure { error ->
                onFailure(error, device)
            }
        }
    }

    private fun onError(error: Throwable, device: Device) {
        when {
            error is CancellationException -> {
                logger.warn("Hue Action was Cancelled while updating hue light: ${device.name}", error)
            }
            error is ApiStatusError && error.code == 207 -> {
                logger.warn("Partial success from hue group update for light: ${device.name}", error)
            }
            error is ApiStatusError -> {
                logger.warn("API Error updating hue light: ${device.name}", error)
            }
            else -> {
                logger.warn("Unknown Error updating hue light: ${device.name}.", error)
            }
        }
    }

    private fun onFailure(error: Throwable, device: Device) {
        when {
            error is ApiStatusError && error.code == 207 -> {
                logger.warn("Hue light update finished with partial success. Ignoring error.")
            }
            else -> {
                logger.error("Hue operation was unable to succeed for light update: ${device.name}", error)
            }
        }
    }
}

internal fun dynamicsForState(action: Action): DynamicsParameters? {
    val offDuration = 5.seconds
    val onDuration = 1.seconds
    val switchState = when (action) {
        is Action.ColorChange -> action.switchState
        is Action.ColorTemperatureChange -> action.switchState
        is Action.Dim -> action.switchState
        is Action.Switch -> action.state
        else -> return null
    }

    return DynamicsParameters(
        duration = when (switchState) {
            SwitchState.OFF -> offDuration
            else -> onDuration
        }
    )
}
