package usonia.rules.lights

import kotlinx.coroutines.test.runBlockingTest
import usonia.foundation.FakeRooms
import usonia.foundation.Room
import usonia.foundation.unit.ColorTemperature
import usonia.kotlin.unit.percent
import java.lang.IllegalStateException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompositeLightingPickerTest {
    @Test
    fun orderedExecution() = runBlockingTest {
        val first = object: LightSettingsPicker {
            override suspend fun getRoomSettings(room: Room): LightSettings {
                return LightSettings.Temperature(ColorTemperature(12), 34.percent)
            }
        }
        val second = object: LightSettingsPicker {
            override suspend fun getRoomSettings(room: Room): LightSettings {
                return LightSettings.Temperature(ColorTemperature(56), 78.percent)
            }
        }

        val composite = CompositeLightingPicker(first, second)

        val result = composite.getRoomSettings(FakeRooms.LivingRoom)

        assertTrue(result is LightSettings.Temperature)
        assertEquals(12, result.temperature.kelvinValue)
        assertEquals(34, result.brightness.percent)
    }

    @Test
    fun skipUnhandled() = runBlockingTest {
        val first = object: LightSettingsPicker {
            override suspend fun getRoomSettings(room: Room): LightSettings {
                return LightSettings.Unhandled
            }
        }
        val second = object: LightSettingsPicker {
            override suspend fun getRoomSettings(room: Room): LightSettings {
                return LightSettings.Temperature(ColorTemperature(56), 78.percent)
            }
        }

        val composite = CompositeLightingPicker(first, second)

        val result = composite.getRoomSettings(FakeRooms.LivingRoom)

        assertTrue(result is LightSettings.Temperature)
        assertEquals(56, result.temperature.kelvinValue)
        assertEquals(78, result.brightness.percent)
    }

    @Test(expected = IllegalStateException::class)
    fun noneHandled() = runBlockingTest {
        val first = object: LightSettingsPicker {
            override suspend fun getRoomSettings(room: Room): LightSettings {
                return LightSettings.Unhandled
            }
        }
        val second = object: LightSettingsPicker {
            override suspend fun getRoomSettings(room: Room): LightSettings {
                return LightSettings.Unhandled
            }
        }

        val composite = CompositeLightingPicker(first, second)

        composite.getRoomSettings(FakeRooms.LivingRoom)
    }
}
