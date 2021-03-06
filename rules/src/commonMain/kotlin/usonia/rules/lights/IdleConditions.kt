package usonia.rules.lights

import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * Indicates a strategy for determining when a room is considered idle.
 */
sealed class IdleConditions {
    /**
     * Specify the amount of time to wait for a room to be considered idle.
     */
    @OptIn(ExperimentalTime::class)
    data class Timed(val time: Duration): IdleConditions()

    /**
     * Indicates that this picker does not specify an idle condition.
     */
    object Unhandled: IdleConditions()

    /**
     * Indicates that no idle handling should occur for this room.
     *
     * This acts differently than [Unhandled] in that it instructs the
     * controller to stop seeking any idle controls for this light from any
     * light settings picker.
     */
    object Ignored: IdleConditions()
}
