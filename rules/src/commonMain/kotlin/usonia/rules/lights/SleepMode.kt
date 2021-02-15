package usonia.rules.lights

import usonia.core.state.getBooleanFlag
import usonia.core.state.hasAdjacentType
import usonia.foundation.Room
import usonia.kotlin.unit.percent
import usonia.server.client.BackendClient

private const val FLAG = "Sleep Mode"

/**
 * Keeps bedroom and adjacent rooms dim/off when sleep mode is enabled.
 */
internal class SleepMode(
    private val client: BackendClient,
): LightSettingsPicker {
    override suspend fun getRoomSettings(room: Room): LightSettings {
        if (!client.getBooleanFlag(FLAG)) return LightSettings.Unhandled

        return when (room.type) {
            Room.Type.Bathroom -> LightSettings.Temperature(
                temperature = Colors.Warm,
                brightness = 5.percent,
            )
            Room.Type.Hallway -> {
                if (client.hasAdjacentType(room, Room.Type.Bedroom)) {
                    LightSettings.Temperature(
                        temperature = Colors.Warm,
                        brightness = 2.percent,
                    )
                } else {
                    LightSettings.Unhandled
                }
            }
            Room.Type.Bedroom -> LightSettings.Ignore
            else -> LightSettings.Unhandled
        }
    }
}
