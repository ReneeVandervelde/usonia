package usonia.rules.greenhouse

import inkapplications.spondee.measure.metric.kelvin
import inkapplications.spondee.scalar.percent
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import usonia.core.state.publishAll
import usonia.foundation.Action
import usonia.foundation.Fixture
import usonia.foundation.SwitchState
import usonia.foundation.findDevicesBy
import usonia.kotlin.*
import usonia.kotlin.datetime.ZonedDateTime
import usonia.server.client.BackendClient
import usonia.server.cron.CronJob
import usonia.server.cron.Schedule
import usonia.weather.WeatherAccess
import kotlin.time.Duration.Companion.hours

private val TARGET_LIGHT_TIME = 18.hours

class MorningPlantLight(
    private val client: BackendClient,
    private val weatherAccess: WeatherAccess,
    private val logger: KimchiLogger = EmptyLogger,
): CronJob {
    override val schedule: Schedule = Schedule().withMinutes { it % 10 == 0 }

    override suspend fun runCron(time: ZonedDateTime) {
        val (devices, forecast) = client.site
            .map { it.findDevicesBy { it.fixture == Fixture.Plant } }
            .combineToPair(weatherAccess.forecast)
            .first()

        val additionalTime = TARGET_LIGHT_TIME - (forecast.sunset - forecast.sunrise)
        val onTime = forecast.sunrise - (additionalTime - 3.hours)
        val offTime = forecast.sunrise + 3.hours

        when {
            time.instant >= onTime && time.instant < offTime -> {
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
            time.instant >= offTime -> {
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
}
