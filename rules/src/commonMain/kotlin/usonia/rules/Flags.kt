package usonia.rules

import com.inkapplications.coroutines.ongoing.map
import usonia.core.state.ConfigurationAccess

object Flags {
    val SleepMode = "Sleep Mode"
    val MovieMode = "Movie Mode"
    val MotionLights = "Motion Lighting"
    val LogAlerts = "Log Alerts"
}

val ConfigurationAccess.sleepMode get() = flags.map { it[Flags.SleepMode]?.toBoolean() ?: false }
