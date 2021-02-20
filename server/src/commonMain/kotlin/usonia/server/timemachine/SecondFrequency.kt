package usonia.server.timemachine

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import usonia.kotlin.datetime.ZonedClock
import usonia.kotlin.datetime.ZonedDateTime
import usonia.kotlin.datetime.ZonedSystemClock
import usonia.kotlin.datetime.current

/**
 * A time-machine that ticks every second.
 *
 * Note: This does not provide second-level accuracy, but will tick roughly
 * each second.
 */
class SecondFrequency(
    clock: ZonedClock = ZonedSystemClock
): TimeMachine {
    override val ticks: Flow<ZonedDateTime> = flow {
        while (true) {
            emit(clock.current)
            delay(1000)
        }
    }
}
