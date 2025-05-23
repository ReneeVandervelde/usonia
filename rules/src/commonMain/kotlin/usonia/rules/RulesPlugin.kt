package usonia.rules

import com.inkapplications.datetime.ZonedClock
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import regolith.processes.cron.CronJob
import regolith.processes.daemon.Daemon
import usonia.celestials.CelestialAccess
import usonia.rules.alerts.DoorAlert
import usonia.rules.alerts.LogErrorAlerts
import usonia.rules.alerts.PipeMonitor
import usonia.rules.alerts.WaterMonitor
import usonia.rules.charging.PowerLimitCharge
import usonia.rules.greenhouse.FanControl
import usonia.rules.greenhouse.HeatControl
import usonia.rules.greenhouse.PlantLight
import usonia.rules.greenhouse.SprinklerControl
import usonia.rules.indicator.Indicator
import usonia.rules.lights.*
import usonia.rules.locks.*
import usonia.server.ServerPlugin
import usonia.server.client.BackendClient
import usonia.server.daemons.ThrottledFailureHandler
import usonia.weather.LocalWeatherAccess

class RulesPlugin(
    client: BackendClient,
    weather: LocalWeatherAccess,
    celestialAccess: CelestialAccess,
    clock: ZonedClock,
    logger: KimchiLogger = EmptyLogger,
): ServerPlugin {
    private val failureHandler = ThrottledFailureHandler()
    private val movieMode = MovieMode(
        client = client,
        logger = logger
    )
    private val sleepMode = SleepMode(client, logger)

    private val colorPicker = CompositeLightingPicker(
        LightingEnabledFlag(client),
        AwayMode(client),
        sleepMode,
        movieMode,
        DayMode(weather),
        CircadianColors(client, weather, logger = logger),
        FixedTimeouts,
        DimmingPhaseHandler,
        OnOffHandler,
    )
    private val fanControl = FanControl(client, failureHandler, logger)

    private val wakeLight = WakeLight(client, client, client, celestialAccess, clock, logger)
    private val sprinkler = SprinklerControl(client, weather, logger)

    override val daemons: List<Daemon> = listOf(
        LogErrorAlerts.also { it.client.value = client },
        WaterMonitor(client, logger),
        LightController(client, colorPicker, logger),
        LightsOffOnSecurityArm(client),
        Indicator(client, weather, logger),
        sleepMode,
        movieMode,
        LockOnSleep(client, logger),
        LockOnSecure(client, logger),
        LockAfterTime(client, logger),
        PipeMonitor(client, logger),
        fanControl,
        HeatControl(client, failureHandler, logger),
        PowerLimitCharge(client, logger),
        DoorAlert(client, logger),
        CodeAlerts(client, logger),
        DisarmOnPrimaryCode(client, logger),
        LockJammed(client, logger),
        wakeLight,
        sprinkler,
    )

    override val crons: List<CronJob> = listOf(
        PlantLight(client, celestialAccess, logger),
        sprinkler,
        wakeLight,
        fanControl,
    )
}
