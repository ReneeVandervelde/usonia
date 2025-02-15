package usonia.rules.lights

import usonia.foundation.Room

/**
 * Determine light color for a room.
 */
internal interface LightSettingsPicker {
    suspend fun getActiveSettings(room: Room): LightSettings = LightSettings.Unhandled
    suspend fun getIdleSettings(room: Room): LightSettings = LightSettings.Unhandled
    suspend fun getStartIdleSettings(room: Room): LightSettings = LightSettings.Unhandled
    suspend fun getIdleConditions(room: Room): IdleConditions = IdleConditions.Unhandled
}
