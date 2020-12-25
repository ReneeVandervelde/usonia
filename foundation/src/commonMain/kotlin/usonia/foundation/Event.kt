package usonia.foundation

import kotlinx.datetime.Instant
import usonia.kotlin.unit.Percentage

/**
 * State changes that have already happened.
 */
sealed class Event {
    companion object Metadata {
        val subClasses = setOf(
            Motion::class,
            Switch::class,
            Temperature::class,
            Humidity::class,
            Lock::class,
            Water::class,
            Latch::class,
            Presence::class,
            Battery::class,
            Tilt::class,
            Movement::class,
        )
    }

    /**
     * The device that was affected by this event.
     */
    abstract val source: Identifier

    abstract fun withSource(source: Identifier): Event

    /**
     * Time that the event occurred.
     */
    abstract val timestamp: Instant

    data class Motion(
        override val source: Identifier,
        override val timestamp: Instant,
        val state: MotionState
    ): Event() {
        override fun withSource(source: Identifier): Event = copy(source = source)
    }

    data class Switch(
        override val source: Identifier,
        override val timestamp: Instant,
        val state: SwitchState
    ): Event() {
        override fun withSource(source: Identifier): Event = copy(source = source)
    }

    data class Temperature(
        override val source: Identifier,
        override val timestamp: Instant,
        val temperature: Float
    ): Event() {
        override fun withSource(source: Identifier): Event = copy(source = source)
    }

    data class Humidity(
        override val source: Identifier,
        override val timestamp: Instant,
        val humidity: Percentage
    ): Event() {
        override fun withSource(source: Identifier): Event = copy(source = source)
    }

    data class Lock(
        override val source: Identifier,
        override val timestamp: Instant,
        val state: LockState,
        val method: LockMethod,
        val code: String?
    ): Event() {
        override fun withSource(source: Identifier): Event = copy(source = source)

        enum class LockMethod { MANUAL, KEYPAD, AUTO, COMMAND }
    }

    data class Water(
        override val source: Identifier,
        override val timestamp: Instant,
        val state: WaterState
    ): Event() {
        override fun withSource(source: Identifier): Event = copy(source = source)
    }

    data class Latch(
        override val source: Identifier,
        override val timestamp: Instant,
        val state: LatchState
    ): Event() {
        override fun withSource(source: Identifier): Event = copy(source = source)
    }

    data class Presence(
        override val source: Identifier,
        override val timestamp: Instant,
        val state: PresenceState
    ): Event() {
        override fun withSource(source: Identifier): Event = copy(source = source)
    }

    data class Battery(
        override val source: Identifier,
        override val timestamp: Instant,
        val percentage: Percentage
    ): Event() {
        override fun withSource(source: Identifier): Event = copy(source = source)
    }

    data class Tilt(
        override val source: Identifier,
        override val timestamp: Instant,
        val x: Float,
        val y: Float,
        val z: Float
    ): Event() {
        override fun withSource(source: Identifier): Event = copy(source = source)
    }

    data class Movement(
        override val source: Identifier,
        override val timestamp: Instant,
        val state: MovementState
    ): Event() {
        override fun withSource(source: Identifier): Event = copy(source = source)
    }
}
