package usonia.rules

import kotlinx.coroutines.flow.map
import usonia.core.state.ConfigurationAccess

object Flags {
    val SleepMode = "Sleep Mode"
}

val ConfigurationAccess.sleepMode get() = flags.map { it[Flags.SleepMode]?.toBoolean() ?: false }
