package usonia.rules

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import usonia.rules.alerts.WaterMonitor
import usonia.core.Daemon
import usonia.core.ServerPlugin
import usonia.core.state.ActionAccess
import usonia.core.state.ActionPublisher
import usonia.core.state.ConfigurationAccess
import usonia.core.state.EventAccess
import usonia.rules.indicator.Indicator
import usonia.rules.lights.CircadianColors
import usonia.rules.lights.LightController
import usonia.weather.WeatherAccess

class RulesPlugin(
    configurationAccess: ConfigurationAccess,
    eventAccess: EventAccess,
    actionPublisher: ActionPublisher,
    actionAccess: ActionAccess,
    weather: WeatherAccess,
    logger: KimchiLogger = EmptyLogger,
): ServerPlugin {
    private val colorPicker = CircadianColors(
        configurationAccess = configurationAccess,
        weather = weather,
        logger = logger,
    )
    override val daemons: List<Daemon> = listOf(
        WaterMonitor(configurationAccess, eventAccess, actionPublisher, logger),
        LightController(configurationAccess, eventAccess, actionPublisher, colorPicker, logger),
        Indicator(weather, configurationAccess, eventAccess, actionPublisher, logger),
    )
}
