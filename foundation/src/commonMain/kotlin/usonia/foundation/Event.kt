package usonia.foundation

import inkapplications.spondee.scalar.Percentage
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import inkapplications.spondee.measure.Power as PowerUnit

/**
 * State changes that have already happened.
 */
@Serializable
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

    abstract val category: EventCategory

    @kotlinx.serialization.Transient
    open val isSensitive: Boolean = true

    @SerialName("Motion")
    @Serializable
    data class Motion(
        override val source: Identifier,
        @Serializable(with = InstantSerializer::class)
        override val timestamp: Instant,
        @SerialName("motionState")
        val state: MotionState
    ): Event() {
        override val isSensitive: Boolean = false
        override val category: EventCategory = EventCategory.Sensor
        override fun withSource(source: Identifier): Event = copy(source = source)
    }

    @SerialName("Switch")
    @Serializable
    data class Switch(
        override val source: Identifier,
        @Serializable(with = InstantSerializer::class)
        override val timestamp: Instant,
        @SerialName("switchState")
        val state: SwitchState
    ): Event() {
        override val isSensitive: Boolean = false
        override val category: EventCategory = EventCategory.Sensor
        override fun withSource(source: Identifier): Event = copy(source = source)
    }

    @Serializable
    @SerialName("Temperature")
    data class Temperature(
        override val source: Identifier,
        @Serializable(with = InstantSerializer::class)
        override val timestamp: Instant,
        @Serializable(with = FahrenheitSerializer::class)
        val temperature: inkapplications.spondee.measure.Temperature
    ): Event() {
        override val isSensitive: Boolean = false
        override val category: EventCategory = EventCategory.Sensor
        override fun withSource(source: Identifier): Event = copy(source = source)
    }

    @Serializable
    @SerialName("Humidity")
    data class Humidity(
        override val source: Identifier,
        @Serializable(with = InstantSerializer::class)
        override val timestamp: Instant,
        @Serializable(with = WholePercentageSerializer::class)
        val humidity: Percentage
    ): Event() {
        override val isSensitive: Boolean = false
        override val category: EventCategory = EventCategory.Sensor
        override fun withSource(source: Identifier): Event = copy(source = source)
    }

    @Serializable
    @SerialName("Lock")
    data class Lock(
        override val source: Identifier,
        @Serializable(with = InstantSerializer::class)
        override val timestamp: Instant,
        @SerialName("lockState")
        val state: LockState,
        @SerialName("lockMethod")
        val method: LockMethod? = null,
        @SerialName("lockCode")
        val code: String? = null,
    ): Event() {
        override val category: EventCategory = when (method) {
            null, LockMethod.MANUAL -> EventCategory.Sensor
            else -> EventCategory.Physical
        }
        override fun withSource(source: Identifier): Event = copy(source = source)

        enum class LockMethod { MANUAL, KEYPAD, AUTO, COMMAND }
    }

    @Serializable
    @SerialName("Water")
    data class Water(
        override val source: Identifier,
        @Serializable(with = InstantSerializer::class)
        override val timestamp: Instant,
        @SerialName("waterState")
        val state: WaterState
    ): Event() {
        override val isSensitive: Boolean = false
        override val category: EventCategory = EventCategory.Sensor
        override fun withSource(source: Identifier): Event = copy(source = source)
    }

    @Serializable
    @SerialName("Latch")
    data class Latch(
        override val source: Identifier,
        @Serializable(with = InstantSerializer::class)
        override val timestamp: Instant,
        @SerialName("latchState")
        val state: LatchState
    ): Event() {
        override val isSensitive: Boolean = false
        override val category: EventCategory = EventCategory.Sensor
        override fun withSource(source: Identifier): Event = copy(source = source)
    }

    @Serializable
    @SerialName("Valve")
    data class Valve(
        override val source: Identifier,
        @Serializable(with = InstantSerializer::class)
        override val timestamp: Instant,
        @SerialName("valveState")
        val state: ValveState
    ): Event() {
        override val isSensitive: Boolean = false
        override val category: EventCategory = EventCategory.Physical
        override fun withSource(source: Identifier): Event = copy(source = source)
    }

    @Serializable
    @SerialName("Presence")
    data class Presence(
        override val source: Identifier,
        @Serializable(with = InstantSerializer::class)
        override val timestamp: Instant,
        @SerialName("presenceState")
        val state: PresenceState
    ): Event() {
        override val category: EventCategory = EventCategory.Sensor
        override fun withSource(source: Identifier): Event = copy(source = source)
    }

    @Serializable
    @SerialName("Battery")
    data class Battery(
        override val source: Identifier,
        @Serializable(with = InstantSerializer::class)
        override val timestamp: Instant,
        @Serializable(with = WholePercentageSerializer::class)
        @SerialName("battery")
        val percentage: Percentage
    ): Event() {
        override val isSensitive: Boolean = false
        override val category: EventCategory = EventCategory.Sensor
        override fun withSource(source: Identifier): Event = copy(source = source)
    }

    @Serializable
    @SerialName("Tilt")
    data class Tilt(
        override val source: Identifier,
        @Serializable(with = InstantSerializer::class)
        override val timestamp: Instant,
        val x: Float,
        val y: Float,
        val z: Float
    ): Event() {
        override val isSensitive: Boolean = false
        override val category: EventCategory = EventCategory.Sensor
        override fun withSource(source: Identifier): Event = copy(source = source)
    }

    @Serializable
    @SerialName("Movement")
    data class Movement(
        override val source: Identifier,
        @Serializable(with = InstantSerializer::class)
        override val timestamp: Instant,
        @SerialName("movementState")
        val state: MovementState
    ): Event() {
        override val isSensitive: Boolean = false
        override val category: EventCategory = EventCategory.Sensor
        override fun withSource(source: Identifier): Event = copy(source = source)
    }

    @Serializable
    @SerialName("Pressure")
    data class Pressure(
        override val source: Identifier,
        @Serializable(with = InstantSerializer::class)
        override val timestamp: Instant,
        val pressure: Float,
    ): Event() {
        override val isSensitive: Boolean = false
        override val category: EventCategory = EventCategory.Sensor
        override fun withSource(source: Identifier): Event = copy(source = source)
    }

    @Serializable
    @SerialName("Power")
    data class Power(
        override val source: Identifier,
        @Serializable(with = InstantSerializer::class)
        override val timestamp: Instant,
        @Serializable(with = RoundedWattSerializer::class)
        val power: PowerUnit,
    ): Event() {
        override val isSensitive: Boolean = false
        override val category: EventCategory = EventCategory.Sensor
        override fun withSource(source: Identifier): Event = copy(source = source)
    }
}
