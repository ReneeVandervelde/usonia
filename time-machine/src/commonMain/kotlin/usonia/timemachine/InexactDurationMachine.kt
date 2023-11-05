package usonia.timemachine

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import usonia.kotlin.datetime.ZonedClock
import usonia.kotlin.datetime.ZonedDateTime
import usonia.kotlin.datetime.ZonedSystemClock
import usonia.kotlin.datetime.current
import kotlin.time.Duration

/**
 * Time machine that emits a tick roughly each [duration] period.
 */
class InexactDurationMachine(
    duration: Duration,
    clock: ZonedClock = ZonedSystemClock,
): TimeMachine {
    override val ticks: Flow<ZonedDateTime> = flow {
        while (true) {
            emit(clock.current)
            delay(duration)
        }
    }
}
