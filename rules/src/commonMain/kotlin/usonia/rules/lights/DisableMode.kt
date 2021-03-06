package usonia.rules.lights

import usonia.core.state.getBooleanFlag
import usonia.foundation.Room
import usonia.server.client.BackendClient

private const val FLAG = "Disable Lights"

/**
 * Disable light controls entirely.
 */
internal class DisableMode(
    private val client: BackendClient,
): LightSettingsPicker {
    override suspend fun getActiveSettings(room: Room): LightSettings {
        if (client.getBooleanFlag(FLAG)) {
            return LightSettings.Ignore
        }

        return LightSettings.Unhandled
    }
}
