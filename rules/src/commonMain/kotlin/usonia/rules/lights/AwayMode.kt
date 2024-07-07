package usonia.rules.lights

import usonia.core.state.getSecurityState
import usonia.foundation.Room
import usonia.foundation.SecurityState
import usonia.server.client.BackendClient

internal class AwayMode(
    val client: BackendClient,
): LightSettingsPicker {
    override suspend fun getActiveSettings(room: Room): LightSettings {
        return when (client.getSecurityState()) {
            SecurityState.Armed -> LightSettings.Ignore
            SecurityState.Disarmed -> LightSettings.Unhandled

        }
    }
}
