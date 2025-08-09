package usonia.foundation

import com.github.ajalt.colormath.model.RGB
import inkapplications.spondee.measure.metric.kelvin
import inkapplications.spondee.scalar.decimalPercentage
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ActionSerializerTests
{
    val json = Json {
        prettyPrint = true
    }
    val actions = listOf(
        TestAction(
            serialized = """
                {
                    "type": "Switch",
                    "target": "test-target",
                    "switchState": "ON"
                }
            """.trimIndent(),
            deserialized = Action.Switch(
                target = Identifier("test-target"),
                state = SwitchState.ON,
            )
        ),
        TestAction(
            serialized = """
                {
                    "type": "Valve",
                    "target": "test-target",
                    "valveState": "OPEN"
                }
            """.trimIndent(),
            deserialized = Action.Valve(
                target = Identifier("test-target"),
                state = ValveState.OPEN,
            )
        ),
        TestAction(
            serialized = """
                {
                    "type": "Dim",
                    "target": "test-target",
                    "dimLevel": 0.8,
                    "switchState": "ON"
                }
            """.trimIndent(),
            deserialized = Action.Dim(
                target = Identifier("test-target"),
                level = 0.8f.decimalPercentage,
                switchState = SwitchState.ON
            )
        ),
        TestAction(
            serialized = """
                {
                    "type": "Dim",
                    "target": "test-target",
                    "dimLevel": 0.8
                }
            """.trimIndent(),
            deserialized = Action.Dim(
                target = Identifier("test-target"),
                level = 0.8f.decimalPercentage
            )
        ),
        TestAction(
            serialized = """
                {
                    "type": "ColorTemperatureChange",
                    "target": "test-target",
                    "colorTemperature": 8,
                    "switchState": "ON",
                    "dimLevel": 0.8
                }
            """.trimIndent(),
            deserialized = Action.ColorTemperatureChange(
                target = Identifier("test-target"),
                temperature = 8.kelvin,
                level = 0.8f.decimalPercentage,
                switchState = SwitchState.ON,
            )
        ),
        TestAction(
            serialized = """
                {
                    "type": "ColorTemperatureChange",
                    "target": "test-target",
                    "colorTemperature": 8
                }
            """.trimIndent(),
            deserialized = Action.ColorTemperatureChange(
                target = Identifier("test-target"),
                temperature = 8.kelvin,
            )
        ),
        TestAction(
            serialized = """
                {
                    "type": "ColorChange",
                    "target": "test-target",
                    "color": [
                        8,
                        16,
                        32
                    ],
                    "switchState": "ON",
                    "dimLevel": 0.8
                }
            """.trimIndent(),
            deserialized = Action.ColorChange(
                target = Identifier("test-target"),
                color = RGB(8, 16, 32),
                level = 0.8f.decimalPercentage,
                switchState = SwitchState.ON,
            )
        ),
        TestAction(
            serialized = """
                {
                    "type": "ColorChange",
                    "target": "test-target",
                    "color": [
                        8,
                        16,
                        32
                    ]
                }
            """.trimIndent(),
            deserialized = Action.ColorChange(
                target = Identifier("test-target"),
                color = RGB(8, 16, 32),
            )
        ),
        TestAction(
            serialized = """
                {
                    "type": "Lock",
                    "target": "test-target",
                    "lockState": "LOCKED"
                }
            """.trimIndent(),
            deserialized = Action.Lock(
                target = Identifier("test-target"),
                state = LockState.LOCKED,
            )
        ),
        TestAction(
            serialized = """
                {
                    "type": "Intent",
                    "target": "test-target",
                    "intentAction": "test-action"
                }
            """.trimIndent(),
            deserialized = Action.Intent(
                target = Identifier("test-target"),
                action = "test-action",
            )
        ),
        TestAction(
            serialized = """
                {
                    "type": "Alert",
                    "target": "test-target",
                    "alertMessage": "test-message",
                    "alertLevel": "Info",
                    "icon": "Bot"
                }
            """.trimIndent(),
            deserialized = Action.Alert(
                target = Identifier("test-target"),
                message = "test-message",
                level = Action.Alert.Level.Info,
                icon = Action.Alert.Icon.Bot
            )
        ),
        TestAction(
            serialized = """
                {
                    "type": "Alert",
                    "target": "test-target",
                    "alertMessage": "test-message",
                    "alertLevel": "Info"
                }
            """.trimIndent(),
            deserialized = Action.Alert(
                target = Identifier("test-target"),
                message = "test-message",
                level = Action.Alert.Level.Info
            )
        ),
    )
    @Test
    fun testSerialization()
    {
        actions.forEach { data ->
            assertEquals(data.deserialized, json.decodeFromString(Action.serializer(), data.serialized), "Action is properly deserialized for ${data.deserialized::class.simpleName}")
            assertEquals(data.serialized, json.encodeToString(Action.serializer(), data.deserialized), "Action is properly serialized for ${data.deserialized::class.simpleName}")
        }
    }
}

data class TestAction<T: Action>(
    val serialized: String,
    val deserialized: T,
)
