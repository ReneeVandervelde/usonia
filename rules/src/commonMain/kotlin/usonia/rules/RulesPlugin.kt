package usonia.rules

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import regolith.processes.cron.CronJob
import regolith.processes.daemon.Daemon
import usonia.rules.alerts.DoorAlert
import usonia.rules.alerts.LogErrorAlerts
import usonia.rules.alerts.PipeMonitor
import usonia.rules.alerts.WaterMonitor
import usonia.rules.charging.PowerLimitCharge
import usonia.rules.greenhouse.FanControl
import usonia.rules.greenhouse.HeatControl
import usonia.rules.greenhouse.MorningPlantLight
import usonia.rules.indicator.Indicator
import usonia.rules.lights.*
import usonia.rules.locks.*
import usonia.rules.locks.LockJammed
import usonia.rules.locks.LockOnAway
import usonia.rules.locks.LockOnSleep
import usonia.server.ServerPlugin
import usonia.server.client.BackendClient
import usonia.weather.WeatherAccess

class RulesPlugin(
    client: BackendClient,
    weather: WeatherAccess,
    logger: KimchiLogger = EmptyLogger,
): ServerPlugin {
    private val movieMode = MovieMode(client, logger)
    private val sleepMode = SleepMode(client, logger)

    private val colorPicker = CompositeLightingPicker(
        LightingEnabledFlag(client),
        AwayMode(client),
        sleepMode,
        movieMode,
        DayMode(weather),
        CircadianColors(client, weather, logger = logger),
        FixedTimeouts,
        OnOffHandler,
    )

    override val daemons: List<Daemon> = listOf(
        LogErrorAlerts.also { it.client.value = client },
        WaterMonitor(client, logger),
        LightController(client, colorPicker, logger),
        Indicator(client, weather, logger),
        sleepMode,
        movieMode,
        LockOnSleep(client, logger),
        LockOnAway(client, logger),
        LockAfterTime(client, logger),
        PipeMonitor(client, logger),
        FanControl(client, logger),
        HeatControl(client, logger),
        PowerLimitCharge(client, logger),
        DoorAlert(client, logger),
        CodeAlerts(client, logger),
        LockJammed(client, logger),
    )

    override val crons: List<CronJob> = listOf(
        MorningPlantLight(client, weather, logger),
    )
}
