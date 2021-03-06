package usonia.rules.lights

import kotlinx.coroutines.test.runBlockingTest
import usonia.foundation.FakeRooms
import usonia.foundation.Room
import usonia.foundation.SwitchState
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
            override suspend fun getIdleSettings(room: Room): LightSettings = LightSettings.Switch(SwitchState.OFF)
        }
        val second = object: LightSettingsPicker {
            override suspend fun getRoomSettings(room: Room): LightSettings {
                return LightSettings.Temperature(ColorTemperature(56), 78.percent)
            }
            override suspend fun getIdleSettings(room: Room): LightSettings = LightSettings.Switch(SwitchState.ON)
        }

        val composite = CompositeLightingPicker(first, second)

        val active = composite.getRoomSettings(FakeRooms.LivingRoom)
        val idle = composite.getIdleSettings(FakeRooms.LivingRoom)

        assertTrue(active is LightSettings.Temperature)
        assertEquals(12, active.temperature.kelvinValue)
        assertEquals(34, active.brightness.percent)
        assertTrue(idle is LightSettings.Switch)
        assertEquals(SwitchState.OFF, idle.state)
    }

    @Test
    fun skipUnhandled() = runBlockingTest {
        val first = object: LightSettingsPicker {
            override suspend fun getRoomSettings(room: Room): LightSettings = LightSettings.Unhandled
            override suspend fun getIdleSettings(room: Room): LightSettings = LightSettings.Unhandled
        }
        val second = object: LightSettingsPicker {
            override suspend fun getRoomSettings(room: Room): LightSettings {
                return LightSettings.Temperature(ColorTemperature(56), 78.percent)
            }
            override suspend fun getIdleSettings(room: Room): LightSettings = LightSettings.Switch(SwitchState.ON)
        }

        val composite = CompositeLightingPicker(first, second)

        val active = composite.getRoomSettings(FakeRooms.LivingRoom)
        val idle = composite.getIdleSettings(FakeRooms.LivingRoom)

        assertTrue(active is LightSettings.Temperature)
        assertEquals(56, active.temperature.kelvinValue)
        assertEquals(78, active.brightness.percent)
        assertTrue(idle is LightSettings.Switch)
        assertEquals(SwitchState.ON, idle.state)
    }

    fun noneHandled() = runBlockingTest {
        val first = object: LightSettingsPicker {
            override suspend fun getRoomSettings(room: Room): LightSettings = LightSettings.Unhandled
            override suspend fun getIdleSettings(room: Room): LightSettings = LightSettings.Unhandled
        }
        val second = object: LightSettingsPicker {
            override suspend fun getRoomSettings(room: Room): LightSettings = LightSettings.Unhandled
            override suspend fun getIdleSettings(room: Room): LightSettings = LightSettings.Unhandled
        }

        val composite = CompositeLightingPicker(first, second)

        val active = runCatching { composite.getRoomSettings(FakeRooms.LivingRoom) }
        val idle = runCatching { composite.getIdleSettings(FakeRooms.LivingRoom) }

        assertTrue(active.isFailure, "Exception thrown when unhandled.")
        assertTrue(idle.isFailure, "Exception thrown when unhandled.")
    }
}
