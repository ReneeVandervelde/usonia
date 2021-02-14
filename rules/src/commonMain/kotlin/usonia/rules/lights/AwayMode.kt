package usonia.rules.lights

import usonia.core.state.allAway
import usonia.core.state.getSite
import usonia.foundation.Room
import usonia.server.client.BackendClient

internal class AwayMode(
    val client: BackendClient,
): LightSettingsPicker {
    override suspend fun getRoomSettings(room: Room): LightSettings {
        val away = client.allAway(client.getSite().users)

        return if (away) LightSettings.Ignore else LightSettings.Unhandled
    }
}
