package usonia.rules.lights

import kotlinx.coroutines.test.runTest
import usonia.foundation.FakeRooms
import usonia.foundation.Room
import usonia.foundation.SwitchState
import usonia.foundation.unit.ColorTemperature
import usonia.kotlin.unit.percent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.minutes

@OptIn(ExperimentalTime::class)
class CompositeLightingPickerTest {
    @Test
    fun orderedExecution() = runTest {
        val first = object: LightSettingsPicker {
            override suspend fun getActiveSettings(room: Room): LightSettings {
                return LightSettings.Temperature(ColorTemperature(12), 34.percent)
            }
            override suspend fun getIdleSettings(room: Room): LightSettings = LightSettings.Switch(SwitchState.OFF)
            override suspend fun getIdleConditions(room: Room): IdleConditions = IdleConditions.Timed(1.minutes)
        }
        val second = object: LightSettingsPicker {
            override suspend fun getActiveSettings(room: Room): LightSettings {
                return LightSettings.Temperature(ColorTemperature(56), 78.percent)
            }
            override suspend fun getIdleSettings(room: Room): LightSettings = LightSettings.Switch(SwitchState.ON)
            override suspend fun getIdleConditions(room: Room): IdleConditions = IdleConditions.Timed(2.minutes)
        }

        val composite = CompositeLightingPicker(first, second)

        val active = composite.getActiveSettings(FakeRooms.LivingRoom)
        val idle = composite.getIdleSettings(FakeRooms.LivingRoom)
        val idleConditions = composite.getIdleConditions(FakeRooms.LivingRoom)

        assertTrue(active is LightSettings.Temperature)
        assertEquals(12, active.temperature.kelvinValue)
        assertEquals(34, active.brightness.percent)
        assertTrue(idle is LightSettings.Switch)
        assertEquals(SwitchState.OFF, idle.state)
        assertTrue(idleConditions is IdleConditions.Timed)
        assertEquals(1.minutes, idleConditions.time)
    }

    @Test
    fun skipUnhandled() = runTest {
        val first = object: LightSettingsPicker {
            override suspend fun getActiveSettings(room: Room): LightSettings = LightSettings.Unhandled
            override suspend fun getIdleSettings(room: Room): LightSettings = LightSettings.Unhandled
            override suspend fun getIdleConditions(room: Room): IdleConditions = IdleConditions.Unhandled
        }
        val second = object: LightSettingsPicker {
            override suspend fun getActiveSettings(room: Room): LightSettings {
                return LightSettings.Temperature(ColorTemperature(56), 78.percent)
            }
            override suspend fun getIdleSettings(room: Room): LightSettings = LightSettings.Switch(SwitchState.ON)
            override suspend fun getIdleConditions(room: Room): IdleConditions = IdleConditions.Timed(2.minutes)
        }

        val composite = CompositeLightingPicker(first, second)

        val active = composite.getActiveSettings(FakeRooms.LivingRoom)
        val idle = composite.getIdleSettings(FakeRooms.LivingRoom)
        val idleConditions = composite.getIdleConditions(FakeRooms.LivingRoom)

        assertTrue(active is LightSettings.Temperature)
        assertEquals(56, active.temperature.kelvinValue)
        assertEquals(78, active.brightness.percent)
        assertTrue(idle is LightSettings.Switch)
        assertEquals(SwitchState.ON, idle.state)
        assertTrue(idleConditions is IdleConditions.Timed)
        assertEquals(2.minutes, idleConditions.time)
    }

    fun noneHandled() = runTest {
        val first = object: LightSettingsPicker {
            override suspend fun getActiveSettings(room: Room): LightSettings = LightSettings.Unhandled
            override suspend fun getIdleSettings(room: Room): LightSettings = LightSettings.Unhandled
            override suspend fun getIdleConditions(room: Room): IdleConditions = IdleConditions.Unhandled
        }
        val second = object: LightSettingsPicker {
            override suspend fun getActiveSettings(room: Room): LightSettings = LightSettings.Unhandled
            override suspend fun getIdleSettings(room: Room): LightSettings = LightSettings.Unhandled
            override suspend fun getIdleConditions(room: Room): IdleConditions = IdleConditions.Unhandled
        }

        val composite = CompositeLightingPicker(first, second)

        val active = runCatching { composite.getActiveSettings(FakeRooms.LivingRoom) }
        val idle = runCatching { composite.getIdleSettings(FakeRooms.LivingRoom) }
        val idleConditions = runCatching { composite.getIdleConditions(FakeRooms.LivingRoom) }

        assertTrue(active.isFailure, "Exception thrown when unhandled.")
        assertTrue(idle.isFailure, "Exception thrown when unhandled.")
        assertTrue(idleConditions.isFailure, "Exception thrown when unhandled.")
    }
}
