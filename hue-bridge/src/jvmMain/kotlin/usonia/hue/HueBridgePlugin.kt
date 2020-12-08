package usonia.hue

import inkapplications.shade.Shade
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import usonia.core.Plugin
import usonia.state.ActionAccess
import usonia.state.ConfigurationAccess

class HueBridgePlugin(
    actionAccess: ActionAccess,
    configurationAccess: ConfigurationAccess,
    logger: KimchiLogger = EmptyLogger,
): Plugin {
    private val shade = Shade(
        appId = "usonia#bridge",
        storage = ConfigurationTokenStorage(configurationAccess, logger),
    )
    override val daemons = listOf(
        HueGroupHandler(actionAccess, configurationAccess, shade.groups, logger),
        ShadeConfigManager(configurationAccess, shade),
    )
}