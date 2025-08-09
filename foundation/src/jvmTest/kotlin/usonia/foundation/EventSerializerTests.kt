package usonia.foundation

import inkapplications.spondee.measure.metric.watts
import inkapplications.spondee.measure.us.fahrenheit
import inkapplications.spondee.scalar.percent
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class EventSerializerTests
{
    val json = Json {
        prettyPrint = true
    }
    val events = listOf(
        TestEvent(
            serialized = """
                {
                    "type": "Motion",
                    "source": "test-source",
                    "timestamp": 123,
                    "motionState": "MOTION"
                }
            """.trimIndent(),
            deserialized = Event.Motion(
                source = Identifier("test-source"),
                timestamp = Instant.fromEpochMilliseconds(123L),
                state = MotionState.MOTION,
            ),
        ),
        TestEvent(
            serialized = """
                {
                    "type": "Switch",
                    "source": "test-source",
                    "timestamp": 123,
                    "switchState": "ON"
                }
            """.trimIndent(),
            deserialized = Event.Switch(
                source = Identifier("test-source"),
                timestamp = Instant.fromEpochMilliseconds(123L),
                state = SwitchState.ON,
            ),
        ),
        TestEvent(
            serialized = """
                {
                    "type": "Temperature",
                    "source": "test-source",
                    "timestamp": 123,
                    "temperature": 8.0
                }
            """.trimIndent(),
            deserialized = Event.Temperature(
                source = Identifier("test-source"),
                timestamp = Instant.fromEpochMilliseconds(123L),
                temperature = (8f).fahrenheit
            ),
        ),
        TestEvent(
            serialized = """
                {
                    "type": "Humidity",
                    "source": "test-source",
                    "timestamp": 123,
                    "humidity": 8.0
                }
            """.trimIndent(),
            deserialized = Event.Humidity(
                source = Identifier("test-source"),
                timestamp = Instant.fromEpochMilliseconds(123L),
                humidity = (8f).percent
            ),
        ),
        TestEvent(
            serialized = """
                {
                    "type": "Lock",
                    "source": "test-source",
                    "timestamp": 123,
                    "lockState": "LOCKED",
                    "lockMethod": "KEYPAD",
                    "lockCode": "1"
                }
            """.trimIndent(),
            deserialized = Event.Lock(
                source = Identifier("test-source"),
                timestamp = Instant.fromEpochMilliseconds(123L),
                state = LockState.LOCKED,
                method = Event.Lock.LockMethod.KEYPAD,
                code = "1"
            ),
        ),
        TestEvent(
            serialized = """
                {
                    "type": "Lock",
                    "source": "test-source",
                    "timestamp": 123,
                    "lockState": "LOCKED"
                }
            """.trimIndent(),
            deserialized = Event.Lock(
                source = Identifier("test-source"),
                timestamp = Instant.fromEpochMilliseconds(123L),
                state = LockState.LOCKED,
                method = null,
                code = null,
            ),
        ),
        TestEvent(
            serialized = """
                {
                    "type": "Water",
                    "source": "test-source",
                    "timestamp": 123,
                    "waterState": "WET"
                }
            """.trimIndent(),
            deserialized = Event.Water(
                source = Identifier("test-source"),
                timestamp = Instant.fromEpochMilliseconds(123L),
                state = WaterState.WET,
            ),
        ),
        TestEvent(
            serialized = """
                {
                    "type": "Latch",
                    "source": "test-source",
                    "timestamp": 123,
                    "latchState": "OPEN"
                }
            """.trimIndent(),
            deserialized = Event.Latch(
                source = Identifier("test-source"),
                timestamp = Instant.fromEpochMilliseconds(123L),
                state = LatchState.OPEN,
            ),
        ),
        TestEvent(
            serialized = """
                {
                    "type": "Valve",
                    "source": "test-source",
                    "timestamp": 123,
                    "valveState": "OPEN"
                }
            """.trimIndent(),
            deserialized = Event.Valve(
                source = Identifier("test-source"),
                timestamp = Instant.fromEpochMilliseconds(123L),
                state = ValveState.OPEN,
            ),
        ),
        TestEvent(
            serialized = """
                {
                    "type": "Presence",
                    "source": "test-source",
                    "timestamp": 123,
                    "presenceState": "AWAY"
                }
            """.trimIndent(),
            deserialized = Event.Presence(
                source = Identifier("test-source"),
                timestamp = Instant.fromEpochMilliseconds(123L),
                state = PresenceState.AWAY,
            ),
        ),
        TestEvent(
            serialized = """
                {
                    "type": "Battery",
                    "source": "test-source",
                    "timestamp": 123,
                    "battery": 8.0
                }
            """.trimIndent(),
            deserialized = Event.Battery(
                source = Identifier("test-source"),
                timestamp = Instant.fromEpochMilliseconds(123L),
                percentage = (8f).percent,
            ),
        ),
        TestEvent(
            serialized = """
                {
                    "type": "Tilt",
                    "source": "test-source",
                    "timestamp": 123,
                    "x": 8.0,
                    "y": 16.0,
                    "z": 32.0
                }
            """.trimIndent(),
            deserialized = Event.Tilt(
                source = Identifier("test-source"),
                timestamp = Instant.fromEpochMilliseconds(123L),
                x = 8f,
                y = 16f,
                z = 32f,
            ),
        ),
        TestEvent(
            serialized = """
                {
                    "type": "Movement",
                    "source": "test-source",
                    "timestamp": 123,
                    "movementState": "MOVING"
                }
            """.trimIndent(),
            deserialized = Event.Movement(
                source = Identifier("test-source"),
                timestamp = Instant.fromEpochMilliseconds(123L),
                state = MovementState.MOVING,
            ),
        ),
        TestEvent(
            serialized = """
                {
                    "type": "Pressure",
                    "source": "test-source",
                    "timestamp": 123,
                    "pressure": 8.0
                }
            """.trimIndent(),
            deserialized = Event.Pressure(
                source = Identifier("test-source"),
                timestamp = Instant.fromEpochMilliseconds(123L),
                pressure = 8f,
            ),
        ),
        TestEvent(
            serialized = """
                {
                    "type": "Power",
                    "source": "test-source",
                    "timestamp": 123,
                    "power": 8
                }
            """.trimIndent(),
            deserialized = Event.Power(
                source = Identifier("test-source"),
                timestamp = Instant.fromEpochMilliseconds(123L),
                power = 8.watts,
            ),
        ),
    )


    @Test
    fun testSerialization()
    {
        events.forEach { data ->
            assertEquals(data.deserialized, json.decodeFromString(Event.serializer(), data.serialized), "Event is properly deserialized for ${data.deserialized::class.simpleName}")
            assertEquals(data.serialized, json.encodeToString(Event.serializer(), data.deserialized), "Event is properly serialized for ${data.deserialized::class.simpleName}")
        }
    }
}

data class TestEvent<T: Event>(
    val serialized: String,
    val deserialized: T,
)
