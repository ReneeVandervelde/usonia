package usonia.glass

import com.inkapplications.glassconsole.structures.DisplayConfig

internal data class UpdateCommand(
    val pluginConfig: GlassPluginConfig,
    val displayConfig: DisplayConfig,
)
