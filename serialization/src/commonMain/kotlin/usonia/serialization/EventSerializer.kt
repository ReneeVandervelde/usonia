package usonia.serialization

import kotlinx.datetime.Instant
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import usonia.foundation.*
import usonia.foundation.unit.Percentage

object EventSerializer: KSerializer<Event> {
    private val serializer = EventJson.serializer()
    override val descriptor: SerialDescriptor = serializer.descriptor

    override fun deserialize(decoder: Decoder): Event {
        val json = decoder.decodeSerializableValue(serializer)
        val id = Uuid(json.source)
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
                json.lockMethod!!,
                json.lockCode
            )
            Event.Temperature::class.simpleName -> Event.Temperature(
                id,
                timestamp,
                json.temperature!!
            )
            Event.Humidity::class.simpleName -> Event.Humidity(
                id,
                timestamp,
                json.humidity!!.let(::Percentage)
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
            Event.Water::class.simpleName -> Event.Water(
                id,
                timestamp,
                json.waterState!!.let { WaterState.valueOf(it) }
            )
            Event.Battery::class.simpleName -> Event.Battery(
                id,
                timestamp,
                json.battery!!.let(::Percentage)
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
                temperature = value.temperature
            )
            is Event.Humidity -> prototype.copy(
                humidity = value.humidity.fraction
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
            is Event.Presence -> prototype.copy(
                presenceState = value.state.name
            )
            is Event.Battery -> prototype.copy(
                battery = value.percentage.fraction
            )
            is Event.Tilt -> prototype.copy(
                x = value.x,
                y = value.y,
                z = value.z
            )
            is Event.Movement -> prototype.copy(
                motionState = value.state.name
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
    val presenceState: String? = null,
    val intent: String? = null,
    val sleepState: String? = null,
    val waterState: String? = null,
    val humidity: Float? = null,
    val battery: Float? = null,
    val x: Float? = null,
    val y: Float? = null,
    val z: Float? = null,
    val movementState: String? = null
)
