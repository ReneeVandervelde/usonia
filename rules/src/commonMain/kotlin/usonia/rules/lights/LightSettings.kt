package usonia.rules.lights

import usonia.foundation.unit.ColorTemperature
import usonia.kotlin.unit.Percentage

internal sealed class LightSettings {
    /**
     * Lighting settings via temperature/brightness.
     */
    data class Temperature(
        val temperature: ColorTemperature,
        val brightness: Percentage,
    ): LightSettings()

    /**
     * Indicates that lights should not be controlled by the application.
     *
     * This acts differently than [Unhandled] in that it instructs the
     * controller to stop seeking any controls for this light from any
     * light settings picker.
     */
    object Ignore: LightSettings()

    /**
     * Indicates that this lighting picker does not handle this light at this time.
     */
    object Unhandled: LightSettings()
}
