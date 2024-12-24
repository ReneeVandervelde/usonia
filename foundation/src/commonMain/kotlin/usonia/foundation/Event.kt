package usonia.foundation

import inkapplications.spondee.measure.metric.watts
import inkapplications.spondee.measure.us.fahrenheit
import inkapplications.spondee.measure.us.toFahrenheit
import inkapplications.spondee.scalar.Percentage
import inkapplications.spondee.scalar.percent
import inkapplications.spondee.scalar.toWholePercentage
import inkapplications.spondee.structure.toFloat
import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import inkapplications.spondee.measure.Power as PowerUnit

/**
 * State changes that have already happened.
 */
@Serializable(with = EventSerializer::class)
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

    open val isSensitive: Boolean = true

    @Serializable(with = EventSerializer::class)
    data class Motion(
        override val source: Identifier,
        override val timestamp: Instant,
        val state: MotionState
    ): Event() {
        override val isSensitive: Boolean = false
        override val category: EventCategory = EventCategory.Sensor
        override fun withSource(source: Identifier): Event = copy(source = source)
    }

    @Serializable(with = EventSerializer::class)
    data class Switch(
        override val source: Identifier,
        override val timestamp: Instant,
        val state: SwitchState
    ): Event() {
        override val isSensitive: Boolean = false
        override val category: EventCategory = EventCategory.Sensor
        override fun withSource(source: Identifier): Event = copy(source = source)
    }

    @Serializable(with = EventSerializer::class)
    data class Temperature(
        override val source: Identifier,
        override val timestamp: Instant,
        val temperature: inkapplications.spondee.measure.Temperature
    ): Event() {
        override val isSensitive: Boolean = false
        override val category: EventCategory = EventCategory.Sensor
        override fun withSource(source: Identifier): Event = copy(source = source)
    }

    @Serializable(with = EventSerializer::class)
    data class Humidity(
        override val source: Identifier,
        override val timestamp: Instant,
        val humidity: Percentage
    ): Event() {
        override val isSensitive: Boolean = false
        override val category: EventCategory = EventCategory.Sensor
        override fun withSource(source: Identifier): Event = copy(source = source)
    }

    @Serializable(with = EventSerializer::class)
    data class Lock(
        override val source: Identifier,
        override val timestamp: Instant,
        val state: LockState,
        val method: LockMethod? = null,
        val code: String?
    ): Event() {
        override val category: EventCategory = when (method) {
            null, LockMethod.MANUAL -> EventCategory.Sensor
            else -> EventCategory.Physical
        }
        override fun withSource(source: Identifier): Event = copy(source = source)

        enum class LockMethod { MANUAL, KEYPAD, AUTO, COMMAND }
    }

    @Serializable(with = EventSerializer::class)
    data class Water(
        override val source: Identifier,
        override val timestamp: Instant,
        val state: WaterState
    ): Event() {
        override val isSensitive: Boolean = false
        override val category: EventCategory = EventCategory.Sensor
        override fun withSource(source: Identifier): Event = copy(source = source)
    }

    @Serializable(with = EventSerializer::class)
    data class Latch(
        override val source: Identifier,
        override val timestamp: Instant,
        val state: LatchState
    ): Event() {
        override val isSensitive: Boolean = false
        override val category: EventCategory = EventCategory.Sensor
        override fun withSource(source: Identifier): Event = copy(source = source)
    }

    @Serializable(with = EventSerializer::class)
    data class Valve(
        override val source: Identifier,
        override val timestamp: Instant,
        val state: ValveState
    ): Event() {
        override val isSensitive: Boolean = false
        override val category: EventCategory = EventCategory.Physical
        override fun withSource(source: Identifier): Event = copy(source = source)
    }

    @Serializable(with = EventSerializer::class)
    data class Presence(
        override val source: Identifier,
        override val timestamp: Instant,
        val state: PresenceState
    ): Event() {
        override val category: EventCategory = EventCategory.Sensor
        override fun withSource(source: Identifier): Event = copy(source = source)
    }

    @Serializable(with = EventSerializer::class)
    data class Battery(
        override val source: Identifier,
        override val timestamp: Instant,
        val percentage: Percentage
    ): Event() {
        override val isSensitive: Boolean = false
        override val category: EventCategory = EventCategory.Sensor
        override fun withSource(source: Identifier): Event = copy(source = source)
    }

    @Serializable(with = EventSerializer::class)
    data class Tilt(
        override val source: Identifier,
        override val timestamp: Instant,
        val x: Float,
        val y: Float,
        val z: Float
    ): Event() {
        override val isSensitive: Boolean = false
        override val category: EventCategory = EventCategory.Sensor
        override fun withSource(source: Identifier): Event = copy(source = source)
    }

    @Serializable(with = EventSerializer::class)
    data class Movement(
        override val source: Identifier,
        override val timestamp: Instant,
        val state: MovementState
    ): Event() {
        override val isSensitive: Boolean = false
        override val category: EventCategory = EventCategory.Sensor
        override fun withSource(source: Identifier): Event = copy(source = source)
    }

    @Serializable(with = EventSerializer::class)
    data class Pressure(
        override val source: Identifier,
        override val timestamp: Instant,
        val pressure: Float,
    ): Event() {
        override val isSensitive: Boolean = false
        override val category: EventCategory = EventCategory.Sensor
        override fun withSource(source: Identifier): Event = copy(source = source)
    }

    @Serializable(with = EventSerializer::class)
    data class Power(
        override val source: Identifier,
        override val timestamp: Instant,
        val power: PowerUnit,
    ): Event() {
        override val isSensitive: Boolean = false
        override val category: EventCategory = EventCategory.Sensor
        override fun withSource(source: Identifier): Event = copy(source = source)
    }
}

object EventSerializer: KSerializer<Event> {
    private val serializer = EventJson.serializer()
    override val descriptor: SerialDescriptor = serializer.descriptor

    override fun deserialize(decoder: Decoder): Event {
        val json = decoder.decodeSerializableValue(serializer)
        val id = Identifier(json.source)
        val timestamp = Instant.fromEpochMilliseconds(json.timestamp)

        return when (json.type) {
            Event.Motion::class.simpleName -> Event.Motion(
                id,
                timestamp,
                json.motionState!!.let { MotionState.valueOf(it) }
            )
            Event.Switch::class.simpleName -> Event.Switch(
                id,
                timestamp,
                json.switchState!!.let { SwitchState.valueOf(it) }
            )
            Event.Lock::class.simpleName -> Event.Lock(
                id,
                timestamp,
                json.lockState!!.let { LockState.valueOf(it) },
                json.lockMethod,
                json.lockCode
            )
            Event.Temperature::class.simpleName -> Event.Temperature(
                id,
                timestamp,
                json.temperature!!.fahrenheit
            )
            Event.Humidity::class.simpleName -> Event.Humidity(
                id,
                timestamp,
                json.humidity!!.percent,
            )
            Event.Presence::class.simpleName -> Event.Presence(
                id,
                timestamp,
                json.presenceState!!.let { PresenceState.valueOf(it) }
            )
            Event.Latch::class.simpleName -> Event.Latch(
                id,
                timestamp,
                json.latchState!!.let { LatchState.valueOf(it) }
            )
            Event.Valve::class.simpleName -> Event.Valve(
                id,
                timestamp,
                json.valveState!!.let { ValveState.valueOf(it) }
            )
            Event.Water::class.simpleName -> Event.Water(
                id,
                timestamp,
                json.waterState!!.let { WaterState.valueOf(it) }
            )
            Event.Battery::class.simpleName -> Event.Battery(
                id,
                timestamp,
                json.battery!!.percent,
            )
            Event.Movement::class.simpleName -> Event.Movement(
                id,
                timestamp,
                json.movementState!!.let { MovementState.valueOf(it) }
            )
            Event.Tilt::class.simpleName -> Event.Tilt(
                id,
                timestamp,
                json.x!!,
                json.y!!,
                json.z!!
            )
            Event.Pressure::class.simpleName -> Event.Pressure(
                id,
                timestamp,
                json.pressure!!,
            )
            Event.Power::class.simpleName -> Event.Power(
                id,
                timestamp,
                json.power!!.watts,
            )
            else -> throw IllegalArgumentException("Unknown type: ${json.type}")
        }
    }

    override fun serialize(encoder: Encoder, value: Event) {
        val prototype = EventJson(
            source = value.source.value,
            timestamp = value.timestamp.toEpochMilliseconds(),
            type = value::class.simpleName!!
        )
        val json = when (value) {
            is Event.Motion -> prototype.copy(
                motionState = value.state.name
            )
            is Event.Switch -> prototype.copy(
                switchState = value.state.name
            )
            is Event.Temperature -> prototype.copy(
                temperature = value.temperature.toFahrenheit().toFloat()
            )
            is Event.Humidity -> prototype.copy(
                humidity = value.humidity.toWholePercentage().value.toFloat(),
            )
            is Event.Lock -> prototype.copy(
                lockState = value.state.name
            )
            is Event.Water -> prototype.copy(
                waterState = value.state.name
            )
            is Event.Latch -> prototype.copy(
                latchState = value.state.name
            )
            is Event.Valve -> prototype.copy(
                valveState = value.state.name
            )
            is Event.Presence -> prototype.copy(
                presenceState = value.state.name
            )
            is Event.Battery -> prototype.copy(
                battery = value.percentage.toWholePercentage().value.toFloat(),
            )
            is Event.Tilt -> prototype.copy(
                x = value.x,
                y = value.y,
                z = value.z
            )
            is Event.Movement -> prototype.copy(
                movementState = value.state.name
            )
            is Event.Pressure -> prototype.copy(
                pressure = value.pressure
            )
            is Event.Power -> prototype.copy(
                power = value.power.toWatts().value.toInt(),
            )
        }

        encoder.encodeSerializableValue(serializer, json)
    }

}

@Serializable
internal data class EventJson(
    val source: String,
    val timestamp: Long,
    val type: String,
    val switchState: String? = null,
    val motionState: String? = null,
    val lockState: String? = null,
    val temperature: Float? = null,
    val lockMethod: Event.Lock.LockMethod? = null,
    val lockCode: String? = null,
    val latchState: String? = null,
    val valveState: String? = null,
    val presenceState: String? = null,
    val intent: String? = null,
    val sleepState: String? = null,
    val waterState: String? = null,
    val humidity: Float? = null,
    val battery: Float? = null,
    val x: Float? = null,
    val y: Float? = null,
    val z: Float? = null,
    val movementState: String? = null,
    val pressure: Float? = null,
    val power: Int? = null,
)
