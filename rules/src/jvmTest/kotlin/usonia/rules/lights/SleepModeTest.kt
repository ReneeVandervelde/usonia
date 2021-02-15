package usonia.rules.lights

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.datetime.*
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.core.state.EventAccessFake
import usonia.foundation.*
import usonia.kotlin.unit.percent
import usonia.server.DummyClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SleepModeTest {
    private val configurationDouble = object: ConfigurationAccess by ConfigurationAccessStub {
        val setFlags = mutableListOf<Pair<String, String?>>()
        override val site: Flow<Site> = flowOf(FakeSite.copy(
            rooms = setOf(FakeRooms.FakeBedroom.copy(
                devices = setOf(FakeDevices.Latch)
            ))
        ))

        override suspend fun setFlag(key: String, value: String?) {
            setFlags += key to value
        }
    }

    @Test
    fun enabled() = runBlockingTest {
        val fakeConfig = object: ConfigurationAccess by configurationDouble {
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
        val fakeConfig = object: ConfigurationAccess by configurationDouble {
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

    @Test
    fun autoEnable() = runBlockingTest {
        val timeZone = TimeZone.UTC
        val clock = object: Clock {
            override fun now(): Instant = LocalDateTime(
                year = 2000,
                monthNumber = 1,
                dayOfMonth = 1,
                hour = 23,
                minute = 0,
                second = 0,
                nanosecond = 0,
            ).toInstant(timeZone)
        }
        val fakeEvents = EventAccessFake()
        val client = DummyClient.copy(
            configurationAccess = configurationDouble,
            eventAccess = fakeEvents,
        )
        val picker = SleepMode(
            client = client,
            clock = clock,
            timeZone = timeZone,
        )

        val daemon = launch { picker.start() }

        fakeEvents.events.emit(Event.Latch(
            source = FakeDevices.Latch.id,
            timestamp = Instant.DISTANT_PAST,
            state = LatchState.CLOSED,
        ))

        assertTrue("Sleep Mode" to "true" in configurationDouble.setFlags, "Sleep mode is set on bedroom door close")

        daemon.cancelAndJoin()
    }

    @Test
    fun autoEnableAfterMidnight() = runBlockingTest {
        val timeZone = TimeZone.UTC
        val clock = object: Clock {
            override fun now(): Instant = LocalDateTime(
                year = 2000,
                monthNumber = 1,
                dayOfMonth = 1,
                hour = 1,
                minute = 0,
                second = 0,
                nanosecond = 0,
            ).toInstant(timeZone)
        }
        val fakeEvents = EventAccessFake()
        val client = DummyClient.copy(
            configurationAccess = configurationDouble,
            eventAccess = fakeEvents,
        )
        val picker = SleepMode(
            client = client,
            clock = clock,
            timeZone = timeZone,
        )

        val daemon = launch { picker.start() }

        fakeEvents.events.emit(Event.Latch(
            source = FakeDevices.Latch.id,
            timestamp = Instant.DISTANT_PAST,
            state = LatchState.CLOSED,
        ))

        assertTrue("Sleep Mode" to "true" in configurationDouble.setFlags, "Sleep mode is set on bedroom door close")

        daemon.cancelAndJoin()
    }

    @Test
    fun skipEnableBeforeNight() = runBlockingTest {
        val timeZone = TimeZone.UTC
        val clock = object: Clock {
            override fun now(): Instant = LocalDateTime(
                year = 2000,
                monthNumber = 1,
                dayOfMonth = 1,
                hour = 8,
                minute = 0,
                second = 0,
                nanosecond = 0,
            ).toInstant(timeZone)
        }
        val fakeEvents = EventAccessFake()
        val client = DummyClient.copy(
            configurationAccess = configurationDouble,
            eventAccess = fakeEvents,
        )
        val picker = SleepMode(
            client = client,
            clock = clock,
            timeZone = timeZone,
        )

        val daemon = launch { picker.start() }

        fakeEvents.events.emit(Event.Latch(
            source = FakeDevices.Latch.id,
            timestamp = Instant.DISTANT_PAST,
            state = LatchState.CLOSED,
        ))

        assertTrue("Sleep Mode" to "true" !in configurationDouble.setFlags, "Sleep mode not set during day")

        daemon.cancelAndJoin()
    }
}
