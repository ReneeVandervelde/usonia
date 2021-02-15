package usonia.rules.lights

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.datetime.*
import usonia.core.state.*
import usonia.foundation.*
import usonia.kotlin.unit.percent
import usonia.server.DummyClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@OptIn(ExperimentalTime::class)
class SleepModeTest {
    private val configurationDouble = object: ConfigurationAccess by ConfigurationAccessStub {
        val setFlags = mutableListOf<Pair<String, String?>>()
        override val site: Flow<Site> = flowOf(FakeSite.copy(
            rooms = setOf(FakeRooms.FakeBedroom.copy(
                devices = setOf(FakeDevices.Latch, FakeDevices.HueGroup)
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

    @Test
    fun lightsOffOnEnable() = runBlockingTest {
        val fakeConfig = object: ConfigurationAccess by configurationDouble {
            override val flags = MutableSharedFlow<Map<String, String?>>()
        }
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = EventAccessStub,
            actionPublisher = actionSpy,
        )
        val picker = SleepMode(client)

        val daemon = launch { picker.start() }
        fakeConfig.flags.emit(mapOf("Sleep Mode" to "true"))
        runCurrent()
        assertEquals(1, actionSpy.actions.size, "Lights Dimmed immediately")
        assertTrue(actionSpy.actions.single() is Action.ColorTemperatureChange)
        assertEquals(FakeDevices.HueGroup.id, actionSpy.actions.single().target)
        advanceTimeBy(31.seconds.toLongMilliseconds())
        assertEquals(2, actionSpy.actions.size, "Lights Turned off after 30 seconds")
        val offAction = actionSpy.actions[1]
        assertTrue(offAction is Action.Switch)
        assertEquals(SwitchState.OFF, offAction.state)


        daemon.cancelAndJoin()
    }

    @Test
    fun noopOnDisable() = runBlockingTest {
        val fakeConfig = object: ConfigurationAccess by configurationDouble {
            override val flags = MutableSharedFlow<Map<String, String?>>()
        }
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = EventAccessStub,
            actionPublisher = actionSpy,
        )
        val picker = SleepMode(client)

        val daemon = launch { picker.start() }
        fakeConfig.flags.emit(mapOf("Sleep Mode" to "false"))
        runCurrent()
        advanceUntilIdle()
        assertEquals(0, actionSpy.actions.size, "No action on disable")

        daemon.cancelAndJoin()
    }

    @Test
    fun autoDisable() = runBlockingTest {
        val client = DummyClient.copy(
            configurationAccess = configurationDouble,
        )
        val picker = SleepMode(client)
        picker.run(Instant.DISTANT_PAST.toLocalDateTime(TimeZone.UTC), TimeZone.UTC)

        assertTrue("Sleep Mode" to "false" in configurationDouble.setFlags, "Sleep mode is set to false on cron run")
    }
}
