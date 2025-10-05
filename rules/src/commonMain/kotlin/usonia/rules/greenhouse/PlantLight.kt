package usonia.rules.greenhouse

import com.inkapplications.coroutines.ongoing.first
import com.inkapplications.datetime.atZone
import inkapplications.spondee.measure.metric.kelvin
import inkapplications.spondee.scalar.percent
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.datetime.*
import regolith.processes.cron.CronJob
import regolith.processes.cron.Schedule
import usonia.celestials.CelestialAccess
import usonia.core.state.findDevicesBy
import usonia.core.state.publishAll
import usonia.foundation.Action
import usonia.foundation.Fixture
import usonia.foundation.SwitchState
import usonia.server.client.BackendClient
import kotlin.time.Duration.Companion.hours

private val TARGET_LIGHT_TIME = 15.hours

class PlantLight(
    private val client: BackendClient,
    private val celestials: CelestialAccess,
    private val logger: KimchiLogger = EmptyLogger,
): CronJob {
    override val schedule: Schedule = Schedule().withMinutes { it % 10 == 0 }

    override suspend fun runCron(time: LocalDateTime, zone: TimeZone) {
        val devices = client.findDevicesBy { it.fixture == Fixture.Plant }
        val today = celestials.localCelestials.first().today

        val onTime = today.daylight.endInclusive - TARGET_LIGHT_TIME
        val offTime = today.daylight.endInclusive
        val now = time.atZone(zone)

        logger.trace("Plant Light onTime: $onTime offTime: $offTime")

        when {
            now >= onTime && now < offTime -> {
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
            now >= offTime -> {
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
