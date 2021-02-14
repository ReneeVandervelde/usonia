package usonia.rules.lights

import usonia.foundation.Room

/**
 * Determine light color for a room.
 */
internal interface LightSettingsPicker {
    suspend fun getRoomSettings(room: Room): LightSettings
}
