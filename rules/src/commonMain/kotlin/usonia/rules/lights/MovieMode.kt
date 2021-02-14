package usonia.rules.lights

import usonia.core.state.ConfigurationAccess
import usonia.core.state.getBooleanFlag
import usonia.foundation.Room
import usonia.foundation.Room.Type.*
import usonia.foundation.unit.ColorTemperature
import usonia.kotlin.unit.percent

/**
 * Dims lights during when a flag is set.
 */
internal class MovieMode(
    private val config: ConfigurationAccess,
): LightSettingsPicker {
    override suspend fun getRoomSettings(room: Room): LightSettings {
        if (!config.getBooleanFlag("Movie Mode")) {
            return LightSettings.Unhandled
        }
        return when(room.type) {
            LivingRoom -> LightSettings.Ignore
            Kitchen, Hallway, Dining -> LightSettings.Temperature(
                temperature = ColorTemperature(2856),
                brightness = 1.percent,
            )
            Bathroom -> LightSettings.Temperature(
                temperature = ColorTemperature(2856),
                brightness = 50.percent,
            )
            Bedroom, Garage, Generic, Office, Storage, Utility -> LightSettings.Unhandled
        }
    }
}
