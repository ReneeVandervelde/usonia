package usonia.rules.greenhouse

import com.inkapplications.coroutines.ongoing.collect
import com.inkapplications.coroutines.ongoing.filter
import com.inkapplications.coroutines.ongoing.filterIsInstance
import inkapplications.spondee.measure.us.fahrenheit
import inkapplications.spondee.measure.us.inches
import inkapplications.spondee.measure.us.toFahrenheit
import inkapplications.spondee.measure.us.toInches
import inkapplications.spondee.scalar.percent
import inkapplications.spondee.spatial.degrees
import inkapplications.spondee.structure.compareTo
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import regolith.processes.cron.CronJob
import regolith.processes.cron.Schedule
import regolith.processes.daemon.Daemon
import usonia.core.client.alertAll
import usonia.core.state.findDevicesBy
import usonia.foundation.Action
import usonia.foundation.Fixture
import usonia.foundation.Identifier
import usonia.foundation.SwitchState
import usonia.foundation.unit.compareTo
import usonia.kotlin.DefaultScope
import usonia.server.client.BackendClient
import usonia.weather.LocalWeatherAccess
import usonia.weather.awaitConditions
import usonia.weather.awaitForecast
import kotlin.time.Duration.Companion.minutes

class SprinklerControl(
    private val client: BackendClient,
    private val weatherAccess: LocalWeatherAccess,
    private val logger: KimchiLogger = EmptyLogger,
    private val backgroundScope: CoroutineScope = DefaultScope(),
): CronJob, Daemon {

    override val schedule: Schedule = Schedule(
        months = setOf(4, 5, 6, 7, 8, 9, 10),
        hours = setOf(6, 7, 13, 18),
        minutes = setOf(0),
    )

    override suspend fun startDaemon(): Nothing {
        client.actions
            .filterIsInstance<Action.Intent>()
            .filter { it.action == "usonia.rules.greenhouse.SprinklerControl.sprinkle" }
            .collect { sprinkle(notifyComplete = it.target) }
    }

    override suspend fun runCron(time: LocalDateTime, zone: TimeZone) {
        val currentConditions = weatherAccess.awaitConditions()
        val currentTemperature = currentConditions.temperature
        val currentForecast = weatherAccess.awaitForecast()
        when {
            currentForecast.rainChance > 40.percent -> {
                logger.debug("Skipping Sprinkler for day with high chance of rain")
                return
            }
            (currentConditions.rainInLast6Hours?.toInches() ?: 0.inches) > 0.5.inches -> {
                logger.debug("Skipping Sprinkler with recent rain")
                return
            }
            currentConditions.isRaining == true -> {
                logger.debug("Skipping Sprinkler while currently raining")
                return
            }
            currentTemperature == null -> {
                logger.warn("Unknown temperature. Skipping Sprinkler to avoid potential freezing damage")
                return
            }
            currentTemperature.toFahrenheit() < 32.fahrenheit -> {
                logger.debug("Skipping Sprinkler for freezing conditions")
                return
            }
            time.date.month <= Month.MAY && currentTemperature.toFahrenheit() > 72.fahrenheit -> {
                logger.debug("Starting Sprinkler seedling watering in warm weather")
                sprinkle()
            }
            time.date.month <= Month.MAY && time.hour <= 12 -> {
                logger.debug("Starting Sprinkler for scheduled watering (every-day seedling)")
                sprinkle()
            }
            currentForecast.highTemperature.toFahrenheit() > 85.fahrenheit && time.hour <= 12 -> {
                logger.debug("Starting Sprinkler for hot day")
                sprinkle()
            }
            time.date.toEpochDays() % 3 == 0 && time.hour <= 12 -> {
                logger.debug("Starting Sprinkler for scheduled watering")
                sprinkle()
            }
            else -> {
                logger.debug("No sprinkler required from conditions")
            }
        }
    }

    private fun sprinkle(
        notifyComplete: Identifier? = null,
    ) {
        backgroundScope.launch {
            client.getSprinklers()
                .also { logger.info("Cycling through ${it.size} sprinklers") }
                .forEach { sprinkler ->
                    logger.info("Turning on ${sprinkler.name} for 15 minutes")
                    client.publishAction(Action.Switch(sprinkler.id, SwitchState.ON))

                    delay(15.minutes)

                    logger.info("Turning off ${sprinkler.name}")
                    client.publishAction(Action.Switch(sprinkler.id, SwitchState.OFF))
                }
            if (notifyComplete != null) {
                client.publishAction(Action.Alert(
                    target = notifyComplete,
                    message = "Garden watering completed!",
                    level = Action.Alert.Level.Info,
                ))
            }
        }
    }

    private suspend fun BackendClient.getSprinklers() = findDevicesBy {
        it.fixture == Fixture.MomentarySprinkler && Action.Switch::class in it.capabilities.actions
    }
}
