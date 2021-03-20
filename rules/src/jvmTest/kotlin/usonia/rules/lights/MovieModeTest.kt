package usonia.rules.lights

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import usonia.core.state.ActionPublisherSpy
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.foundation.*
import usonia.kotlin.OngoingFlow
import usonia.kotlin.asOngoing
import usonia.kotlin.ongoingFlowOf
import usonia.kotlin.unit.percent
import usonia.server.DummyClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MovieModeTest {
    @Test
    fun enabled() = runBlockingTest {
        val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
            override val flags: OngoingFlow<Map<String, String?>> = ongoingFlowOf(mapOf(
                "Movie Mode" to "true"
            ))
        }
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
        )
        val picker = MovieMode(client)

        val livingRoom = picker.getActiveSettings(FakeRooms.LivingRoom)
        assertTrue(livingRoom is LightSettings.Ignore)

        val hallway = picker.getActiveSettings(FakeRooms.FakeHallway)
        assertTrue(hallway is LightSettings.Temperature)
        assertEquals(1.percent, hallway.brightness)
        assertEquals(Colors.Warm, hallway.temperature)

        val bathroom = picker.getActiveSettings(FakeRooms.FakeBathroom)
        assertTrue(bathroom is LightSettings.Temperature)
        assertEquals(50.percent, bathroom.brightness)
        assertEquals(Colors.Warm, bathroom.temperature)

        val bedroom = picker.getActiveSettings(FakeRooms.FakeBedroom)
        assertTrue(bedroom is LightSettings.Unhandled)
    }

    @Test
    fun disabled() = runBlockingTest {
        val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
            override val flags: OngoingFlow<Map<String, String?>> = ongoingFlowOf(mapOf(
                "Movie Mode" to "false"
            ))
        }
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
        )
        val picker = MovieMode(client)

        val livingRoom = picker.getActiveSettings(FakeRooms.LivingRoom)
        assertTrue(livingRoom is LightSettings.Unhandled)

        val hallway = picker.getActiveSettings(FakeRooms.FakeHallway)
        assertTrue(hallway is LightSettings.Unhandled)

        val bathroom = picker.getActiveSettings(FakeRooms.FakeBathroom)
        assertTrue(bathroom is LightSettings.Unhandled)

        val bedroom = picker.getActiveSettings(FakeRooms.FakeBedroom)
        assertTrue(bedroom is LightSettings.Unhandled)
    }

    @Test
    fun unspecified() = runBlockingTest {
        val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
            override val flags: OngoingFlow<Map<String, String?>> = ongoingFlowOf(mapOf())
        }
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
        )
        val picker = MovieMode(client)

        val livingRoom = picker.getActiveSettings(FakeRooms.LivingRoom)
        assertTrue(livingRoom is LightSettings.Unhandled)

        val hallway = picker.getActiveSettings(FakeRooms.FakeHallway)
        assertTrue(hallway is LightSettings.Unhandled)

        val bathroom = picker.getActiveSettings(FakeRooms.FakeBathroom)
        assertTrue(bathroom is LightSettings.Unhandled)

        val bedroom = picker.getActiveSettings(FakeRooms.FakeBedroom)
        assertTrue(bedroom is LightSettings.Unhandled)
    }

    @Test
    fun dropFirst() = runBlockingTest {
        val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site: OngoingFlow<Site> = ongoingFlowOf(FakeSite.copy(
                rooms = setOf(FakeRooms.LivingRoom.copy(
                    devices = setOf(FakeDevices.HueGroup),
                )),
            ))
            val mutableFlags = MutableSharedFlow<Map<String, String?>>()
            override val flags = mutableFlags.asOngoing()
        }
        val publisherSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            actionPublisher = publisherSpy,
        )
        val picker = MovieMode(client)

        val daemon = launch { picker.start() }
        fakeConfig.mutableFlags.emit(mapOf(
            "Movie Mode" to "true"
        ))

        assertEquals(0, publisherSpy.actions.size, "No action taken on initial event at start-up")

        daemon.cancelAndJoin()
    }

    @Test
    fun start() = runBlockingTest {
        val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site: OngoingFlow<Site> = ongoingFlowOf(FakeSite.copy(
                rooms = setOf(FakeRooms.LivingRoom.copy(
                    devices = setOf(FakeDevices.HueGroup),
                )),
            ))
            val mutableFlags = MutableSharedFlow<Map<String, String?>>()
            override val flags = mutableFlags.asOngoing()
        }
        val publisherSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            actionPublisher = publisherSpy,
        )
        val picker = MovieMode(client)

        val daemon = launch { picker.start() }
        fakeConfig.mutableFlags.emit(mapOf(
            "Movie Mode" to "false"
        ))
        fakeConfig.mutableFlags.emit(mapOf(
            "Movie Mode" to "true"
        ))

        assertEquals(1, publisherSpy.actions.size)
        val action = publisherSpy.actions.single()
        assertTrue(action is Action.Switch)
        assertEquals(SwitchState.OFF, action.state)

        daemon.cancelAndJoin()
    }

    @Test
    fun stop() = runBlockingTest {
        val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site: OngoingFlow<Site> = ongoingFlowOf(FakeSite.copy(
                rooms = setOf(FakeRooms.LivingRoom.copy(
                    devices = setOf(FakeDevices.HueGroup),
                )),
            ))
            val mutableFlags = MutableSharedFlow<Map<String, String?>>()
            override val flags = mutableFlags.asOngoing()
        }
        val publisherSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            actionPublisher = publisherSpy,
        )
        val picker = MovieMode(client)

        val daemon = launch { picker.start() }
        fakeConfig.mutableFlags.emit(mapOf(
            "Movie Mode" to "true"
        ))
        fakeConfig.mutableFlags.emit(mapOf(
            "Movie Mode" to "false"
        ))

        assertEquals(1, publisherSpy.actions.size)
        val action = publisherSpy.actions.single()
        assertTrue(action is Action.Switch)
        assertEquals(SwitchState.ON, action.state)

        daemon.cancelAndJoin()
    }
}
