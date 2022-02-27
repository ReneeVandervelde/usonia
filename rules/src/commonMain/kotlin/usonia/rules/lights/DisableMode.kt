package usonia.rules.lights

import usonia.core.state.getBooleanFlag
import usonia.foundation.Room
import usonia.rules.Flags
import usonia.server.client.BackendClient

/**
 * Disable light controls entirely.
 */
internal class DisableMode(
    private val client: BackendClient,
): LightSettingsPicker {
    override suspend fun getActiveSettings(room: Room): LightSettings {
        if (client.getBooleanFlag(Flags.DisableMode)) {
            return LightSettings.Ignore
        }

        return LightSettings.Unhandled
    }

    override suspend fun getIdleSettings(room: Room): LightSettings {
        if (client.getBooleanFlag(Flags.DisableMode)) {
            return LightSettings.Ignore
        }

        return LightSettings.Unhandled
    }
}
