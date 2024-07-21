package usonia.glass

import com.inkapplications.glassconsole.structures.pin.ChallengeResponse
import com.inkapplications.glassconsole.structures.pin.Nonce
import usonia.core.client.DeviceProperty
import usonia.foundation.Event

internal data class DisplayViewModel(
    val config: GlassPluginConfig,
    val flags: Flags,
    val doorStates: List<DeviceProperty<Event.Latch?>>,
    val security: SecurityInfo,
) {
    data class Flags(
        val sleepEnabled: Boolean,
        val movieEnabled: Boolean,
    )

    data class ChallengeData(
        val nonce: Nonce,
        val witness: ChallengeResponse,
    )

    data class SecurityInfo(
        val challenge: ChallengeData?,
        val isArming: Boolean,
        val armDelayMinutes: Int,
    )
}
