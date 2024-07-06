package usonia.glass

import com.inkapplications.glassconsole.structures.pin.Pin
import com.inkapplications.glassconsole.structures.pin.Psk
import usonia.foundation.Identifier

data class GlassPluginConfig(
    val bridgeId: Identifier,
    val homeIp: String,
    val psk: Psk,
    val pin: Pin,
    val type: DisplayType,
) {
    enum class DisplayType {
        Large,
        Small,
    }
}
