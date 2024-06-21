package usonia.glass

import com.inkapplications.glassconsole.structures.DisplayConfig
import usonia.foundation.Bridge

internal data class UpdateCommand(
    val bridge: Bridge,
    val config: DisplayConfig,
)
