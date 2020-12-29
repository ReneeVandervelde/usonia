package usonia.server.timemachine

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock as Clock
import kotlinx.datetime.*

/**
 * A time-machine that ticks every second.
 *
 * Note: This does not provide second-level accuracy, but will tick roughly
 * each second.
 */
internal object SecondFrequency: TimeMachine {
    override val ticks: Flow<LocalDateTime> = flow {
        while (true) {
            emit(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))
            delay(1000)
        }
    }
}
