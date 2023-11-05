package usonia.timemachine

import usonia.kotlin.datetime.ZonedClock
import usonia.kotlin.datetime.ZonedSystemClock
import kotlin.time.Duration.Companion.seconds

/**
 * A time-machine that ticks roughly every second.
 */
class SecondFrequency(
    clock: ZonedClock = ZonedSystemClock
): TimeMachine by InexactDurationMachine(1.seconds, clock)

