package usonia.rules.lights

import usonia.foundation.unit.ColorTemperature
import usonia.kotlin.unit.Percentage

/**
 * Settings for lighting.
 */
internal data class LightSettings(
    val temperature: ColorTemperature,
    val brightness: Percentage,
)
