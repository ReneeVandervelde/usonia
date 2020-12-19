package usonia.hue

import inkapplications.shade.Shade
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import usonia.core.ServerPlugin
import usonia.core.state.ActionAccess
import usonia.core.state.ConfigurationAccess

class HueBridgePlugin(
    actionAccess: ActionAccess,
    configurationAccess: ConfigurationAccess,
    logger: KimchiLogger = EmptyLogger,
): ServerPlugin {
    private val shade = Shade(
        appId = "usonia#bridge",
        storage = ConfigurationTokenStorage(configurationAccess, logger),
    )
    override val daemons = listOf(
        HueGroupHandler(actionAccess, configurationAccess, shade.groups, logger),
        ShadeConfigManager(configurationAccess, shade),
    )
}
