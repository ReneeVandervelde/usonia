package usonia.rules

import usonia.core.state.ConfigurationAccess
import usonia.kotlin.map

object Flags {
    val SleepMode = "Sleep Mode"
}

val ConfigurationAccess.sleepMode get() = flags.map { it[Flags.SleepMode]?.toBoolean() ?: false }
