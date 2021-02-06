package usonia.rules.lights

import usonia.foundation.unit.ColorTemperature
import usonia.kotlin.unit.Percentage

internal sealed class LightSettings {
    data class Temperature(
        val temperature: ColorTemperature,
        val brightness: Percentage,
    ): LightSettings()

    object Unhandled: LightSettings()
}
