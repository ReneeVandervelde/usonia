package usonia.rules.lights

import usonia.foundation.Room
import usonia.foundation.SwitchState

internal object OnOffHandler: LightSettingsPicker {
    override suspend fun getActiveSettings(room: Room): LightSettings {
        return LightSettings.Switch(SwitchState.ON)
    }

    override suspend fun getIdleSettings(room: Room): LightSettings {
        return LightSettings.Switch(SwitchState.OFF)
    }
}
