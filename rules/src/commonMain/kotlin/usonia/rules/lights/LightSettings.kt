package usonia.rules.lights

import inkapplications.spondee.measure.ColorTemperature
import inkapplications.spondee.scalar.Percentage
import usonia.foundation.SwitchState

internal sealed interface LightSettings
{
    /**
     * Lighting settings via temperature/brightness.
     */
    data class Temperature(
        val temperature: ColorTemperature,
        val brightness: Percentage,
    ): LightSettings

    /**
     * Lighting settings that only change brightness.
     */
    data class Brightness(
        val brightness: Percentage,
    ): LightSettings

    /**
     * Indicates that a light should be toggled on/off without any settings.
     */
    data class Switch(val state: SwitchState): LightSettings

    /**
     * Indicates that lights should not be controlled by the application.
     *
     * This acts differently than [Unhandled] in that it instructs the
     * controller to stop seeking any controls for this light from any
     * light settings picker.
     */
    data object Ignore: LightSettings

    /**
     * Indicates that this lighting picker does not handle this light at this time.
     */
    data object Unhandled: LightSettings
}
