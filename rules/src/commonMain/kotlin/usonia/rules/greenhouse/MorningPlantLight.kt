package usonia.rules.greenhouse

import inkapplications.spondee.measure.metric.kelvin
import inkapplications.spondee.scalar.percent
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import regolith.processes.cron.CronJob
import regolith.processes.cron.Schedule
import usonia.core.state.publishAll
import usonia.foundation.Action
import usonia.foundation.Fixture
import usonia.foundation.SwitchState
import usonia.foundation.findDevicesBy
import usonia.kotlin.*
import usonia.kotlin.datetime.ZonedDateTime
import usonia.kotlin.datetime.withZone
import usonia.server.client.BackendClient
import usonia.weather.Forecast
import usonia.weather.LocalWeatherAccess
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours

private val TARGET_LIGHT_TIME = 18.hours

class MorningPlantLight(
    private val client: BackendClient,
    private val weatherAccess: LocalWeatherAccess,
    private val logger: KimchiLogger = EmptyLogger,
): CronJob {
    override val schedule: Schedule = Schedule().withMinutes { it % 10 == 0 }

    override suspend fun runCron(time: LocalDateTime, zone: TimeZone) {
        val (devices, forecast) = client.site
            .map { it.findDevicesBy { it.fixture == Fixture.Plant } }
            .combineToPair(weatherAccess.forecast)
            .first()

        // Fix for late forecast updates in the morning.
        val sunrise = forecast.sunriseToday(time.withZone(zone))
        val sunset = forecast.nextSunset(time.withZone(zone))
        val additionalTime = TARGET_LIGHT_TIME - (sunset - sunrise)
        val onTime = sunrise - (additionalTime - 3.hours)
        val offTime = sunrise + 3.hours
        val instant = time.toInstant(zone)

        logger.trace("Sunrise is ${sunrise} onTime: $onTime offTime: $offTime")

        when {
            instant >= onTime && instant < offTime -> {
                logger.trace("Turning on ${devices.size} Morning Plant Lights")
                devices.map {
                    Action.ColorTemperatureChange(
                        target = it.id,
                        temperature = 6504.kelvin,
                        switchState = SwitchState.ON,
                        level = 100.percent,
                    )
                }.run { client.publishAll(this) }
            }
            instant >= offTime -> {
                logger.trace("Turning off ${devices.size} Morning Plant Lights")
                devices.map {
                    Action.Switch(it.id, SwitchState.OFF)
                }.run { client.publishAll(this) }
            }
            else -> {
                logger.trace("No action for plant lights before hours.")
            }
        }
    }

    private fun Forecast.sunriseToday(now: ZonedDateTime): Instant {
        return when {
            sunrise < now.instant && sunset < now.instant -> sunrise + 1.days
            else -> sunrise
        }
    }

    private fun Forecast.nextSunset(now: ZonedDateTime): Instant {
        return when {
            sunrise < now.instant && sunset < now.instant -> sunset + 1.days
            else -> sunset
        }
    }

}
