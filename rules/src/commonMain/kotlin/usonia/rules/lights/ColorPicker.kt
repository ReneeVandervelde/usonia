package usonia.rules.lights

import usonia.foundation.Room

/**
 * Determine light color for a room.
 */
internal interface ColorPicker {
    suspend fun getRoomColor(room: Room): LightSettings
}
