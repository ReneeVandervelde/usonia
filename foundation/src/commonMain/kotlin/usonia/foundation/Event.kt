package usonia.foundation

import kotlinx.datetime.Instant
import usonia.foundation.unit.Percentage

/**
 * State changes that have already happened.
 */
sealed class Event {
    /**
     * The device that was affected by this event.
     */
    abstract val source: Uuid

    /**
     * Time that the event occurred.
     */
    abstract val timestamp: Instant

    data class Motion(
        override val source: Uuid,
        override val timestamp: Instant,
        val state: MotionState
    ): Event()

    data class Switch(
        override val source: Uuid,
        override val timestamp: Instant,
        val state: SwitchState
    ): Event()

    data class Temperature(
        override val source: Uuid,
        override val timestamp: Instant,
        val temperature: Float
    ): Event()

    data class Humidity(
        override val source: Uuid,
        override val timestamp: Instant,
        val humidity: Percentage
    ): Event()

    data class Lock(
        override val source: Uuid,
        override val timestamp: Instant,
        val state: LockState,
        val method: LockMethod,
        val code: String?
    ): Event() {
        enum class LockMethod { MANUAL, KEYPAD, AUTO, COMMAND }
    }

    data class Water(
        override val source: Uuid,
        override val timestamp: Instant,
        val state: WaterState
    ): Event()

    data class Latch(
        override val source: Uuid,
        override val timestamp: Instant,
        val state: LatchState
    ): Event()

    data class Presence(
        override val source: Uuid,
        override val timestamp: Instant,
        val state: PresenceState
    ): Event()

    data class Battery(
        override val source: Uuid,
        override val timestamp: Instant,
        val percentage: Percentage
    ): Event()

    data class Tilt(
        override val source: Uuid,
        override val timestamp: Instant,
        val x: Float,
        val y: Float,
        val z: Float
    ): Event()

    data class Movement(
        override val source: Uuid,
        override val timestamp: Instant,
        val state: MovementState
    ): Event()
}
