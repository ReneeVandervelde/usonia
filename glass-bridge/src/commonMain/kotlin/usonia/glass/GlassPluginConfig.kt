package usonia.glass

import com.inkapplications.glassconsole.structures.pin.Pin
import com.inkapplications.glassconsole.structures.pin.Psk
import usonia.foundation.Identifier

data class GlassPluginConfig(
    val bridgeId: Identifier,
    val homeIp: String,
    val deviceIp: String,
    val psk: Psk,
    val pin: Pin,
    val type: DisplayType,
    val sleepMode: DisplayMode,
    val movieMode: DisplayMode,
) {
    enum class DisplayType {
        Large,
        Small,
    }
    enum class DisplayMode {
        Off,
        Dim,
        Normal,
    }
}
