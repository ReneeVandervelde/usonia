package usonia.rules.lights

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.foundation.FakeRooms
import usonia.foundation.FakeSite
import usonia.foundation.Site
import usonia.kotlin.unit.percent
import usonia.server.DummyClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SleepModeTest {
    @Test
    fun enabled() = runBlockingTest {
        val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site: Flow<Site> = flowOf(FakeSite.copy(
                rooms = setOf(FakeRooms.FakeBedroom)
            ))
            override val flags: Flow<Map<String, String?>> = flowOf(
                mapOf("Sleep Mode" to "true")
            )
        }
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
        )
        val picker = SleepMode(client)

        val bedroom = picker.getRoomSettings(FakeRooms.FakeBedroom)
        val livingRoom = picker.getRoomSettings(FakeRooms.LivingRoom)
        val tranquilHallwayResult = picker.getRoomSettings(FakeRooms.FakeHallway.copy(
            adjacentRooms = setOf(FakeRooms.FakeBedroom.id)
        ))
        val unaffectedHallwayResult = picker.getRoomSettings(FakeRooms.FakeHallway)
        val bathroom = picker.getRoomSettings(FakeRooms.FakeBathroom)

        assertTrue(tranquilHallwayResult is LightSettings.Temperature)
        assertEquals(Colors.Warm, tranquilHallwayResult.temperature)
        assertEquals(2.percent, tranquilHallwayResult.brightness)
        assertTrue(bathroom is LightSettings.Temperature)
        assertEquals(Colors.Warm, bathroom.temperature)
        assertEquals(5.percent, bathroom.brightness)
        assertTrue(bedroom is LightSettings.Ignore)
        assertTrue(livingRoom is LightSettings.Unhandled)
        assertTrue(unaffectedHallwayResult is LightSettings.Unhandled)
    }

    @Test
    fun disabled() = runBlockingTest {
        val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site: Flow<Site> = flowOf(FakeSite.copy(
                rooms = setOf(FakeRooms.FakeBedroom)
            ))
            override val flags: Flow<Map<String, String?>> = flowOf(
                mapOf("Sleep Mode" to "false")
            )
        }
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
        )
        val picker = SleepMode(client)

        val bedroom = picker.getRoomSettings(FakeRooms.FakeBedroom)
        val livingRoom = picker.getRoomSettings(FakeRooms.LivingRoom)
        val tranquilHallwayResult = picker.getRoomSettings(FakeRooms.FakeHallway.copy(
            adjacentRooms = setOf(FakeRooms.FakeBedroom.id)
        ))
        val unaffectedHallwayResult = picker.getRoomSettings(FakeRooms.FakeHallway)
        val bathroom = picker.getRoomSettings(FakeRooms.FakeBathroom)

        assertTrue(tranquilHallwayResult is LightSettings.Unhandled)
        assertTrue(bathroom is LightSettings.Unhandled)
        assertTrue(bedroom is LightSettings.Unhandled)
        assertTrue(livingRoom is LightSettings.Unhandled)
        assertTrue(unaffectedHallwayResult is LightSettings.Unhandled)
    }
}
