package usonia.rules.lights

import inkapplications.spondee.scalar.percent
import usonia.foundation.Room
import usonia.foundation.Room.Type.*

internal object DimmingPhaseHandler: LightSettingsPicker
{
    override suspend fun getStartIdleSettings(room: Room): LightSettings {
        return when (room.type) {
            Hallway, Bedroom -> LightSettings.Brightness(
                brightness = 10.percent,
            )
            else -> LightSettings.Brightness(
                brightness = 15.percent,
            )
        }
    }
}
