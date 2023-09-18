package usonia.rules.greenhouse

import inkapplications.spondee.measure.metric.kelvin
import inkapplications.spondee.scalar.percent
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import usonia.core.state.publishAll
import usonia.foundation.Action
import usonia.foundation.Fixture
import usonia.foundation.SwitchState
import usonia.foundation.findDevicesBy
import usonia.kotlin.*
import usonia.server.Daemon
import usonia.server.client.BackendClient
import usonia.weather.WeatherAccess
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

private val TARGET_LIGHT_TIME = 17.hours

class MorningPlantLight(
    private val client: BackendClient,
    private val weatherAccess: WeatherAccess,
    private val clock: Clock = Clock.System,
    private val logger: KimchiLogger = EmptyLogger,
): Daemon {
    override suspend fun start(): Nothing {
        client.site
            .map { it.findDevicesBy { it.fixture == Fixture.Plant } }
            .combineToPair(weatherAccess.forecast)
            .collectLatest { (devices, forecast) ->
                val additionalTime = TARGET_LIGHT_TIME - (forecast.sunset - forecast.sunrise)
                val onTime = forecast.sunrise - (additionalTime - 2.hours)
                val offTime = forecast.sunrise + 2.hours
                while (clock.now().let { it < onTime || it < offTime }) {
                    val startWaitTime = onTime - clock.now()
                    if (startWaitTime > 0.milliseconds) {
                        logger.debug("Waiting $startWaitTime to turn ON Morning Plant Lights")
                        delay(startWaitTime)
                    }
                    logger.info("Turning on Morning Plant Lights")
                    devices.map {
                        Action.ColorTemperatureChange(
                            target = it.id,
                            temperature = 6504.kelvin,
                            switchState = SwitchState.ON,
                            level = 100.percent,
                        )
                    }.run { client.publishAll(this) }

                    val endWaitTime = offTime - clock.now()
                    if (endWaitTime > 0.milliseconds) {
                        logger.debug("Waiting $endWaitTime to turn OFF Morning Plant Lights")
                        delay(endWaitTime)
                    }
                    logger.info("Turning off Morning Plant Lights")
                    devices.map {
                        Action.Switch(it.id, SwitchState.OFF)
                    }.run { client.publishAll(this) }

                }
                logger.trace("Morning Plant Light after scheduled hours")
            }
    }
}
