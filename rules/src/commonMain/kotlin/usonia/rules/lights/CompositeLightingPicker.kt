package usonia.rules.lights

import usonia.foundation.Room
import usonia.rules.lights.LightSettings.Unhandled

/**
 * Combines multiple light pickers into one.
 *
 * This executes an array of pickers and takes the first result that is
 * not [Unhandled] by the delegated picker.
 *
 * @param pickers an ordered set of lighting pickers to delegate to.
 */
internal class CompositeLightingPicker(
    private vararg val pickers: LightSettingsPicker,
): LightSettingsPicker {
    override suspend fun getRoomSettings(room: Room): LightSettings {
        pickers.forEach {
            val result = it.getRoomSettings(room)
            if (result != Unhandled) return result
        }
        throw IllegalStateException("No Picker handled room color!")
    }

    override suspend fun getIdleSettings(room: Room): LightSettings {
        pickers.forEach {
            val result = it.getIdleSettings(room)
            if (result != Unhandled) return result
        }
        throw IllegalStateException("No Picker handled room Idle settings!")
    }
}
