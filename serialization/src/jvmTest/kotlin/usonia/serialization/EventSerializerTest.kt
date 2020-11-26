package usonia.serialization

import kotlinx.serialization.json.Json
import usonia.foundation.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventSerializerTest {
    @Test
    fun motion() {
        val json = """
{
    "source": "fake-id",
    "timestamp": 123,
    "type": "Motion",
    "motionState": "MOTION"
}
        """

        val result = Json.decodeFromString(EventSerializer, json)

        assertEquals("fake-id", result.source.value)
        assertEquals(123, result.timestamp.toEpochMilliseconds())
        assertTrue(result is Event.Motion)
        assertEquals(MotionState.MOTION, result.state)
    }

    @Test
    fun switch() {
        val json = """
{
    "source": "fake-id",
    "timestamp": 123,
    "type": "Switch",
    "switchState": "OFF"
}
        """

        val result = Json.decodeFromString(EventSerializer, json)

        assertEquals("fake-id", result.source.value)
        assertEquals(123, result.timestamp.toEpochMilliseconds())
        assertTrue(result is Event.Switch)
        assertEquals(SwitchState.OFF, result.state)
    }

    @Test
    fun temperature() {
        val json = """
{
    "source": "fake-id",
    "timestamp": 123,
    "type": "Temperature",
    "temperature": 65.5
}
        """

        val result = Json.decodeFromString(EventSerializer, json)

        assertEquals("fake-id", result.source.value)
        assertEquals(123, result.timestamp.toEpochMilliseconds())
        assertTrue(result is Event.Temperature)
        assertEquals(65.5f, result.temperature)
    }

    @Test
    fun humidity() {
        val json = """
{
    "source": "fake-id",
    "timestamp": 123,
    "type": "Humidity",
    "humidity": .69
}
        """

        val result = Json.decodeFromString(EventSerializer, json)

        assertEquals("fake-id", result.source.value)
        assertEquals(123, result.timestamp.toEpochMilliseconds())
        assertTrue(result is Event.Humidity)
        assertEquals(.69f, result.humidity.fraction)
    }

    @Test
    fun lock() {
        val json = """
{
    "source": "fake-id",
    "timestamp": 123,
    "type": "Lock",
    "lockState": "LOCKED",
    "lockMethod": "AUTO",
    "lockCode": "test"
}
        """

        val result = Json.decodeFromString(EventSerializer, json)

        assertEquals("fake-id", result.source.value)
        assertEquals(123, result.timestamp.toEpochMilliseconds())
        assertTrue(result is Event.Lock)
        assertEquals(LockState.LOCKED, result.state)
        assertEquals(Event.Lock.LockMethod.AUTO, result.method)
        assertEquals("test", result.code)
    }

    @Test
    fun water() {
        val json = """
{
    "source": "fake-id",
    "timestamp": 123,
    "type": "Water",
    "waterState": "WET"
}
        """

        val result = Json.decodeFromString(EventSerializer, json)

        assertEquals("fake-id", result.source.value)
        assertEquals(123, result.timestamp.toEpochMilliseconds())
        assertTrue(result is Event.Water)
        assertEquals(WaterState.WET, result.state)
    }

    @Test
    fun latch() {
        val json = """
{
    "source": "fake-id",
    "timestamp": 123,
    "type": "Latch",
    "latchState": "CLOSED"
}
        """

        val result = Json.decodeFromString(EventSerializer, json)

        assertEquals("fake-id", result.source.value)
        assertEquals(123, result.timestamp.toEpochMilliseconds())
        assertTrue(result is Event.Latch)
        assertEquals(LatchState.CLOSED, result.state)
    }

    @Test
    fun presence() {
        val json = """
{
    "source": "fake-id",
    "timestamp": 123,
    "type": "Presence",
    "presenceState": "AWAY"
}
        """

        val result = Json.decodeFromString(EventSerializer, json)

        assertEquals("fake-id", result.source.value)
        assertEquals(123, result.timestamp.toEpochMilliseconds())
        assertTrue(result is Event.Presence)
        assertEquals(PresenceState.AWAY, result.state)
    }

    @Test
    fun battery() {
        val json = """
{
    "source": "fake-id",
    "timestamp": 123,
    "type": "Battery",
    "battery": .69
}
        """

        val result = Json.decodeFromString(EventSerializer, json)

        assertEquals("fake-id", result.source.value)
        assertEquals(123, result.timestamp.toEpochMilliseconds())
        assertTrue(result is Event.Battery)
        assertEquals(.69f, result.percentage.fraction)
    }

    @Test
    fun tilt() {
        val json = """
{
    "source": "fake-id",
    "timestamp": 123,
    "type": "Tilt",
    "x": 420,
    "y": .69,
    "z": 6.66
}
        """

        val result = Json.decodeFromString(EventSerializer, json)

        assertEquals("fake-id", result.source.value)
        assertEquals(123, result.timestamp.toEpochMilliseconds())
        assertTrue(result is Event.Tilt)
        assertEquals(420f, result.x)
        assertEquals(.69f, result.y)
        assertEquals(6.66f, result.z)
    }

    @Test
    fun movement() {
        val json = """
{
    "source": "fake-id",
    "timestamp": 123,
    "type": "Movement",
    "movementState": "MOVING"
}
        """

        val result = Json.decodeFromString(EventSerializer, json)

        assertEquals("fake-id", result.source.value)
        assertEquals(123, result.timestamp.toEpochMilliseconds())
        assertTrue(result is Event.Movement)
        assertEquals(MovementState.MOVING, result.state)
    }
}
