package usonia.rules.greenhouse

import inkapplications.spondee.measure.us.fahrenheit
import inkapplications.spondee.measure.us.inches
import inkapplications.spondee.measure.us.toFahrenheit
import inkapplications.spondee.measure.us.toInches
import inkapplications.spondee.scalar.percent
import inkapplications.spondee.structure.compareTo
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import regolith.processes.cron.CronJob
import regolith.processes.cron.Schedule
import usonia.core.state.findDevicesBy
import usonia.core.state.publishAll
import usonia.foundation.*
import usonia.foundation.unit.compareTo
import usonia.kotlin.DefaultScope
import usonia.server.client.BackendClient
import usonia.weather.WeatherAccess
import java.time.DayOfWeek.*
import kotlin.time.Duration.Companion.minutes

class SprinklerControl(
    private val client: BackendClient,
    private val weatherAccess: WeatherAccess,
    private val logger: KimchiLogger = EmptyLogger,
    private val backgroundScope: CoroutineScope = DefaultScope(),
): CronJob {

    override val schedule: Schedule = Schedule(
        months = setOf(4, 5, 6, 7, 8, 9, 10),
        hours = setOf(6, 7),
        minutes = setOf(0),
    )

    override suspend fun runCron(time: LocalDateTime, zone: TimeZone) {
        when {
            weatherAccess.currentForecast.rainChance > 30.percent -> {
                logger.debug("Skipping Sprinkler for day with high chance of rain")
                return
            }
            weatherAccess.currentConditions.rainInLast6Hours.toInches() > 0.5.inches -> {
                logger.debug("Skipping Sprinkler with recent rain")
                return
            }
            weatherAccess.currentConditions.isRaining -> {
                logger.debug("Skipping Sprinkler while currently raining")
                return
            }
            weatherAccess.currentConditions.temperature < 50.fahrenheit -> {
                logger.debug("Skipping Sprinkler for cold day")
                return
            }
            weatherAccess.currentForecast.lowTemperature.toFahrenheit() < 50.fahrenheit -> {
                logger.debug("Skipping Sprinkler for cold forecast")
                return
            }
            time.date.toEpochDays() % 3 == 0 -> {
                logger.debug("Starting Sprinkler for scheduled watering")
                sprinkle()
            }
            weatherAccess.currentForecast.highTemperature.toFahrenheit() > 85.fahrenheit -> {
                logger.debug("Starting Sprinkler for hot day")
                sprinkle()
            }
        }
        logger.debug("No sprinkler required from conditions")
    }

    private suspend fun sprinkle() {
        client.getSprinklers()
            .also { logger.info("Turning on ${it.size} sprinklers") }
            .map { Action.Switch(it.id, SwitchState.ON) }
            .run { client.publishAll(this) }
        backgroundScope.launch {
            delay(10.minutes)
            client.getSprinklers()
                .also { logger.info("Turning off ${it.size} sprinklers") }
                .map { Action.Switch(it.id, SwitchState.OFF) }
                .run { client.publishAll(this) }
        }
    }

    private suspend fun BackendClient.getSprinklers() = findDevicesBy {
        it.fixture == Fixture.MomentarySprinkler && Action.Switch::class in it.capabilities.actions
    }
}
