package usonia.serialization

import kotlinx.serialization.json.Json
import usonia.foundation.Action
import usonia.foundation.LockState
import usonia.foundation.SwitchState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ActionSerializerTest {
    @Test
    fun switch() {
        val json = """
            {
                "type": "Switch",
                "target": "test-target",
                "switchState": "OFF"
            }
        """

        val result = Json.decodeFromString(ActionSerializer, json)

        assertEquals("test-target", result.target.value)
        assertTrue(result is Action.Switch)
        assertEquals(SwitchState.OFF, result.state)
    }

    @Test
    fun dim() {
        val json = """
            {
                "type": "Dim",
                "target": "test-target",
                "dimLevel": .69,
                "switchState": "ON"
            }
        """

        val result = Json.decodeFromString(ActionSerializer, json)

        assertEquals("test-target", result.target.value)
        assertTrue(result is Action.Dim)
        assertEquals(.69f, result.level.fraction)
        assertEquals(SwitchState.ON, result.switchState)
    }

    @Test
    fun colorTemperature() {
        val json = """
            {
                "type": "ColorTemperatureChange",
                "target": "test-target",
                "colorTemperature": 420,
                "dimLevel": .69,
                "switchState": "ON"
            }
        """

        val result = Json.decodeFromString(ActionSerializer, json)

        assertEquals("test-target", result.target.value)
        assertTrue(result is Action.ColorTemperatureChange)
        assertEquals(420, result.temperature.kelvinValue)
        assertEquals(.69f, result.level?.fraction)
        assertEquals(SwitchState.ON, result.switchState)
    }

    @Test
    fun color() {
        val json = """
            {
                "type": "ColorChange",
                "target": "test-target",
                "color": [1, 2, 3]
                "dimLevel": .69,
                "switchState": "ON"
            }
        """

        val result = Json.decodeFromString(ActionSerializer, json)

        assertEquals("test-target", result.target.value)
        assertTrue(result is Action.ColorChange)
        assertEquals(1, result.color.toRGB().r)
        assertEquals(2, result.color.toRGB().g)
        assertEquals(3, result.color.toRGB().b)
        assertEquals(.69f, result.level?.fraction)
        assertEquals(SwitchState.ON, result.switchState)
    }

    @Test
    fun lock() {
        val json = """
            {
                "type": "Lock",
                "target": "test-target",
                "lockState": "LOCKED"
            }
        """

        val result = Json.decodeFromString(ActionSerializer, json)

        assertEquals("test-target", result.target.value)
        assertTrue(result is Action.Lock)
        assertEquals(LockState.LOCKED, result.state)
    }

    @Test
    fun intent() {
        val json = """
            {
                "type": "Intent",
                "target": "test-target",
                "intentAction": "test-action"
            }
        """

        val result = Json.decodeFromString(ActionSerializer, json)

        assertEquals("test-target", result.target.value)
        assertTrue(result is Action.Intent)
        assertEquals("test-action", result.action)
    }

    @Test
    fun alert() {
        val json = """
            {
                "type": "Alert",
                "target": "test-target",
                "alertMessage": "test-message"
            }
        """

        val result = Json.decodeFromString(ActionSerializer, json)

        assertEquals("test-target", result.target.value)
        assertTrue(result is Action.Alert)
        assertEquals("test-message", result.message)
    }
}
