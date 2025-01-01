package usonia.rules.greenhouse

import com.inkapplications.coroutines.ongoing.combinePair
import com.inkapplications.coroutines.ongoing.first
import com.inkapplications.coroutines.ongoing.map
import com.inkapplications.datetime.ZonedDateTime
import com.inkapplications.datetime.atZone
import inkapplications.spondee.measure.metric.kelvin
import inkapplications.spondee.scalar.percent
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.datetime.*
import regolith.processes.cron.CronJob
import regolith.processes.cron.Schedule
import usonia.core.state.publishAll
import usonia.foundation.Action
import usonia.foundation.Fixture
import usonia.foundation.SwitchState
import usonia.foundation.findDevicesBy
import usonia.server.client.BackendClient
import usonia.weather.FullForecast
import usonia.weather.LocalWeatherAccess
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

private val TARGET_LIGHT_TIME = 15.hours

class MorningPlantLight(
    private val client: BackendClient,
    private val weatherAccess: LocalWeatherAccess,
    private val logger: KimchiLogger = EmptyLogger,
): CronJob {
    override val schedule: Schedule = Schedule().withMinutes { it % 10 == 0 }

    override suspend fun runCron(time: LocalDateTime, zone: TimeZone) {
        val (devices, forecast) = client.site
            .map { it.findDevicesBy { it.fixture == Fixture.Plant } }
            .combinePair(weatherAccess.forecast)
            .first()

        // Fix for late forecast updates in the morning.
        val sunrise = forecast.sunriseToday(time.atZone(zone))
        val sunset = forecast.sunsetToday(time.atZone(zone))
        val additionalTime = (TARGET_LIGHT_TIME - (sunset - sunrise)).takeIf { it > 0.hours } ?: 0.hours
        val offset = 30.minutes
        val onTime = sunrise - additionalTime + offset
        val offTime = sunset + offset
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

    private fun FullForecast.sunriseToday(now: ZonedDateTime): Instant {
        return when {
            sunrise.toLocalDateTime(now.zone).date > now.localDate -> sunrise - 1.days
            sunrise.toLocalDateTime(now.zone).date < now.localDate -> sunrise + 1.days
            else -> sunrise
        }
    }

    private fun FullForecast.sunsetToday(now: ZonedDateTime): Instant {
        return when {
            sunset.toLocalDateTime(now.zone).date > now.localDate -> sunset - 1.days
            sunset.toLocalDateTime(now.zone).date < now.localDate -> sunset + 1.days
            else -> sunset
        }
    }
}
