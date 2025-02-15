package usonia.rules.lights

import usonia.core.state.getBooleanFlag
import usonia.foundation.Room
import usonia.rules.Flags
import usonia.server.client.BackendClient

/**
 * Controls whether smart lighting is enabled at all.
 */
internal class LightingEnabledFlag(
    private val client: BackendClient,
): LightSettingsPicker {
    override suspend fun getActiveSettings(room: Room): LightSettings {
        if (client.getBooleanFlag(Flags.MotionLights, default = true)) {
            return LightSettings.Unhandled
        }

        return LightSettings.Ignore
    }

    override suspend fun getIdleSettings(room: Room): LightSettings {
        if (client.getBooleanFlag(Flags.MotionLights, default = true)) {
            return LightSettings.Unhandled
        }

        return LightSettings.Ignore
    }

    override suspend fun getStartIdleSettings(room: Room): LightSettings {
        if (client.getBooleanFlag(Flags.MotionLights, default = true)) {
            return LightSettings.Unhandled
        }

        return LightSettings.Ignore
    }
}
