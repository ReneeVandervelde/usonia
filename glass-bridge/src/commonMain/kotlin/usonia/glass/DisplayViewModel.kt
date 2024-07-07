package usonia.glass

import com.inkapplications.glassconsole.structures.pin.ChallengeResponse
import com.inkapplications.glassconsole.structures.pin.Nonce
import usonia.core.client.DeviceProperty
import usonia.foundation.Event

internal sealed interface DisplayViewModel {
    val config: GlassPluginConfig
    val flags: Flags
    val doorStates: List<DeviceProperty<Event.Latch?>>
    val challenge: ChallengeData?
    val security: SecurityInfo

    data class Small(
        override val config: GlassPluginConfig,
        override val flags: Flags,
        override val doorStates: List<DeviceProperty<Event.Latch?>>,
        override val challenge: ChallengeData?,
        override val security: SecurityInfo,
    ): DisplayViewModel

    data class Large(
        override val config: GlassPluginConfig,
        override val flags: Flags,
        override val doorStates: List<DeviceProperty<Event.Latch?>>,
        override val challenge: ChallengeData?,
        override val security: SecurityInfo,
    ): DisplayViewModel

    data class Flags(
        val sleepEnabled: Boolean,
        val movieEnabled: Boolean,
    )

    data class ChallengeData(
        val nonce: Nonce,
        val witness: ChallengeResponse,
    )

    data class SecurityInfo(
        val isArming: Boolean,
        val armDelayMinutes: Int,
    )
}
