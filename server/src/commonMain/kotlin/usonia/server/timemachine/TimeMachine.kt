package usonia.server.timemachine

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import usonia.kotlin.datetime.ZonedDateTime

/**
 * A time-repeating event system.
 */
internal interface TimeMachine {
    /**
     * Flow that emits every time a recurring time unit has passed.
     *
     * Note: Because this flow will be consumed as a suspending operation,
     * this provided *frequency* but not *accuracy* – that is, the results
     * can be expected at roughly incremental times, but are not guaranteed
     * to be resumed at their exact clock mark.
     */
    val ticks: Flow<ZonedDateTime>
}

/**
 * Filter ticks to minute-level frequency.
 */
internal val TimeMachine.minutes get() = ticks.distinctUntilChangedBy { it.minute }
