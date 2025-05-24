package usonia.rules.lights

import com.inkapplications.coroutines.ongoing.*
import com.inkapplications.datetime.ZonedClock
import com.inkapplications.datetime.ZonedDateTime
import com.inkapplications.datetime.atZone
import inkapplications.spondee.scalar.decimalPercentage
import inkapplications.spondee.scalar.percent
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import regolith.processes.cron.CronJob
import regolith.processes.cron.Schedule
import regolith.processes.daemon.Daemon
import usonia.celestials.CelestialAccess
import usonia.core.state.ActionAccess
import usonia.core.state.ActionPublisher
import usonia.core.state.ConfigurationAccess
import usonia.core.state.getSite
import usonia.foundation.*
import usonia.foundation.unit.interpolate
import usonia.kotlin.DefaultScope
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class WakeLight(
    private val configurationAccess: ConfigurationAccess,
    private val actionAccess: ActionAccess,
    private val actionPublisher: ActionPublisher,
    private val celestialAccess: CelestialAccess,
    private val clock: ZonedClock,
    private val logger: KimchiLogger,
    private val backgroundScope: CoroutineScope = DefaultScope(),
): CronJob, Daemon {
    override val schedule: Schedule = Schedule()
        .withMinutes { it % 5 == 0 }
    private val dismissAfter = 2.hours
    private val clamp = LocalTime(5, 30)..LocalTime(7, 15)
    private val offset = 30.minutes
    private val startColor = Colors.Warm
    private val endColor = Colors.Daylight
    private val lastDismissed = MutableStateFlow<LocalDate?>(null)

    override suspend fun startDaemon(): Nothing {
        actionAccess.actions
            .filterIsInstance<Action.Intent>()
            .filter { it.action == "usonia.rules.lights.WakeLight.dismiss" }
            .collect {
                backgroundScope.launch {
                    dismiss(today = clock.localDate())
                }
            }
    }

    private suspend fun dismiss(today: LocalDate)
    {
        if (lastDismissed.value == today) {
            logger.trace("Already dismissed today. Ignoring.")
            return
        }
        val wakeLights = configurationAccess.getSite().findDevicesBy { it.fixture == Fixture.WakeLight }
        wakeLights.map {
            actionPublisher.publishAction(Action.ColorTemperatureChange(
                target = it.id,
                temperature = Colors.Warm,
                level = 1.percent,
                switchState = SwitchState.ON,
            ))
        }
        delay(5.seconds)
        wakeLights.map {
            actionPublisher.publishAction(Action.Switch(
                target = it.id,
                state = SwitchState.OFF,
            ))
        }
        lastDismissed.value = today
    }

    override suspend fun runCron(time: LocalDateTime, zone: TimeZone) {
        val now = time.atZone(zone)
        val celestials = celestialAccess.localCelestials.first()
        val startWithOffset = celestials.today.civilTwilight.start + offset
        val earlyClamp = LocalDateTime(now.localDate, clamp.start).atZone(zone)
        val lateClamp = LocalDateTime(now.localDate, clamp.endInclusive).atZone(zone)
        val startTime = min(max(startWithOffset, earlyClamp), lateClamp)
        val span = ((celestials.today.daylight.start.instant + offset) - startTime.instant).coerceAtLeast(15.minutes)
        val wakeLights = configurationAccess.getSite().findDevicesBy { it.fixture == Fixture.WakeLight }

        if (now < startTime) {
            logger.trace("Before wake time, not running wake light.")
            return
        }
        if (lastDismissed.value == now.localDate) {
            logger.trace("Wake light dismissed today, not running.")
            return
        }
        if (now >= celestials.today.daylight.start + offset + dismissAfter) {
            logger.trace("After wake time, dismissing.")
            dismiss(now.localDate)
            return
        }

        val progress = ((now.instant - startTime.instant) / span).let { min(1.0, it) }
        val currentBrightness = max(0.01, progress).decimalPercentage
        val currentColor = interpolate(startColor, endColor, progress)

        wakeLights.map {
            actionPublisher.publishAction(Action.ColorTemperatureChange(
                target = it.id,
                temperature = currentColor,
                switchState = SwitchState.ON,
                level = currentBrightness,
            ))
        }
    }

    private fun max(a: ZonedDateTime, b: ZonedDateTime): ZonedDateTime {
        return if (a > b) a else b
    }

    private fun min(a: ZonedDateTime, b: ZonedDateTime): ZonedDateTime {
        return if (a < b) a else b
        }
}
