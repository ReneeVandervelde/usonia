package usonia.rules.lights

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.foundation.FakeRooms
import usonia.kotlin.unit.percent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MovieModeTest {
    @Test
    fun enabled() = runBlockingTest {
        val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
            override val flags: Flow<Map<String, String?>> = flowOf(mapOf(
                "Movie Mode" to "true"
            ))
        }
        val picker = MovieMode(fakeConfig)

        val livingRoom = picker.getRoomColor(FakeRooms.LivingRoom)
        assertTrue(livingRoom is LightSettings.Temperature)
        assertEquals(0.percent, livingRoom.brightness)
        assertEquals(2856, livingRoom.temperature.kelvinValue)

        val hallway = picker.getRoomColor(FakeRooms.FakeHallway)
        assertTrue(hallway is LightSettings.Temperature)
        assertEquals(1.percent, hallway.brightness)
        assertEquals(2856, hallway.temperature.kelvinValue)

        val bathroom = picker.getRoomColor(FakeRooms.FakeBathroom)
        assertTrue(bathroom is LightSettings.Temperature)
        assertEquals(50.percent, bathroom.brightness)
        assertEquals(2856, bathroom.temperature.kelvinValue)

        val bedroom = picker.getRoomColor(FakeRooms.FakeBedroom)
        assertTrue(bedroom is LightSettings.Unhandled)
    }

    @Test
    fun disabled() = runBlockingTest {
        val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
            override val flags: Flow<Map<String, String?>> = flowOf(mapOf(
                "Movie Mode" to "false"
            ))
        }
        val picker = MovieMode(fakeConfig)

        val livingRoom = picker.getRoomColor(FakeRooms.LivingRoom)
        assertTrue(livingRoom is LightSettings.Unhandled)

        val hallway = picker.getRoomColor(FakeRooms.FakeHallway)
        assertTrue(hallway is LightSettings.Unhandled)

        val bathroom = picker.getRoomColor(FakeRooms.FakeBathroom)
        assertTrue(bathroom is LightSettings.Unhandled)

        val bedroom = picker.getRoomColor(FakeRooms.FakeBedroom)
        assertTrue(bedroom is LightSettings.Unhandled)
    }

    @Test
    fun unspecified() = runBlockingTest {
        val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
            override val flags: Flow<Map<String, String?>> = flowOf(mapOf())
        }
        val picker = MovieMode(fakeConfig)

        val livingRoom = picker.getRoomColor(FakeRooms.LivingRoom)
        assertTrue(livingRoom is LightSettings.Unhandled)

        val hallway = picker.getRoomColor(FakeRooms.FakeHallway)
        assertTrue(hallway is LightSettings.Unhandled)

        val bathroom = picker.getRoomColor(FakeRooms.FakeBathroom)
        assertTrue(bathroom is LightSettings.Unhandled)

        val bedroom = picker.getRoomColor(FakeRooms.FakeBedroom)
        assertTrue(bedroom is LightSettings.Unhandled)
    }
}
