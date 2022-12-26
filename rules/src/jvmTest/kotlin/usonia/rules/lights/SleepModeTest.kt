package usonia.rules.lights

import inkapplications.spondee.scalar.percent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.*
import usonia.core.state.*
import usonia.foundation.*
import usonia.kotlin.OngoingFlow
import usonia.kotlin.asOngoing
import usonia.kotlin.datetime.*
import usonia.kotlin.ongoingFlowOf
import usonia.server.DummyClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class SleepModeTest {
    private val configurationDouble = object: ConfigurationAccess by ConfigurationAccessStub {
        val setFlags = mutableListOf<Pair<String, String?>>()
        override val site: OngoingFlow<Site> = ongoingFlowOf(FakeSite.copy(
            rooms = setOf(FakeRooms.FakeBedroom.copy(
                devices = setOf(FakeDevices.Latch, FakeDevices.HueGroup)
            ))
        ))

        override suspend fun setFlag(key: String, value: String?) {
            setFlags += key to value
        }
    }

    private val night = object: ZonedClock by UtcClock {
        override fun now(): Instant = parseLocalDateTime("2020-01-02T23:00:00").instant
    }

    private val morning = object: ZonedClock by UtcClock {
        override fun now(): Instant = parseLocalDateTime("2020-01-02T05:00:00").instant
    }

    @Test
    fun enabled() = runTest {
        val fakeConfig = object: ConfigurationAccess by configurationDouble {
            override val flags: OngoingFlow<Map<String, String?>> = ongoingFlowOf(
                mapOf("Sleep Mode" to "true")
            )
        }
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
        )
        val picker = SleepMode(client, clock = night)

        val bedroom = picker.getActiveSettings(FakeRooms.FakeBedroom)
        val livingRoom = picker.getActiveSettings(FakeRooms.LivingRoom)
        val tranquilHallwayResult = picker.getActiveSettings(FakeRooms.FakeHallway.copy(
            adjacentRooms = setOf(FakeRooms.FakeBedroom.id)
        ))
        val unaffectedHallwayResult = picker.getActiveSettings(FakeRooms.FakeHallway)
        val bathroom = picker.getActiveSettings(FakeRooms.FakeBathroom)

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
    fun enabledMorning() = runTest {
        val fakeConfig = object: ConfigurationAccess by configurationDouble {
            override val flags: OngoingFlow<Map<String, String?>> = ongoingFlowOf(
                mapOf("Sleep Mode" to "true")
            )
        }
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
        )
        val picker = SleepMode(client, clock = morning)

        val bedroom = picker.getActiveSettings(FakeRooms.FakeBedroom)
        val livingRoom = picker.getActiveSettings(FakeRooms.LivingRoom)
        val tranquilHallwayResult = picker.getActiveSettings(FakeRooms.FakeHallway.copy(
            adjacentRooms = setOf(FakeRooms.FakeBedroom.id)
        ))
        val unaffectedHallwayResult = picker.getActiveSettings(FakeRooms.FakeHallway)
        val bathroom = picker.getActiveSettings(FakeRooms.FakeBathroom)

        assertTrue(tranquilHallwayResult is LightSettings.Temperature)
        assertEquals(Colors.Warm, tranquilHallwayResult.temperature)
        assertEquals(2.percent, tranquilHallwayResult.brightness)
        assertTrue(bathroom is LightSettings.Unhandled)
        assertTrue(bedroom is LightSettings.Ignore)
        assertTrue(livingRoom is LightSettings.Unhandled)
        assertTrue(unaffectedHallwayResult is LightSettings.Unhandled)
    }

    @Test
    fun disabled() = runTest {
        val fakeConfig = object: ConfigurationAccess by configurationDouble {
            override val flags: OngoingFlow<Map<String, String?>> = ongoingFlowOf(
                mapOf("Sleep Mode" to "false")
            )
        }
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
        )
        val picker = SleepMode(client)

        val bedroom = picker.getActiveSettings(FakeRooms.FakeBedroom)
        val livingRoom = picker.getActiveSettings(FakeRooms.LivingRoom)
        val tranquilHallwayResult = picker.getActiveSettings(FakeRooms.FakeHallway.copy(
            adjacentRooms = setOf(FakeRooms.FakeBedroom.id)
        ))
        val unaffectedHallwayResult = picker.getActiveSettings(FakeRooms.FakeHallway)
        val bathroom = picker.getActiveSettings(FakeRooms.FakeBathroom)

        assertTrue(tranquilHallwayResult is LightSettings.Unhandled)
        assertTrue(bathroom is LightSettings.Unhandled)
        assertTrue(bedroom is LightSettings.Unhandled)
        assertTrue(livingRoom is LightSettings.Unhandled)
        assertTrue(unaffectedHallwayResult is LightSettings.Unhandled)
    }

    @Test
    fun autoEnable() = runTest {
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
            clock = clock.withTimeZone(timeZone),
        )

        val daemon = launch { picker.start() }
        runCurrent()

        fakeEvents.mutableEvents.emit(Event.Latch(
            source = FakeDevices.Latch.id,
            timestamp = Instant.DISTANT_PAST,
            state = LatchState.CLOSED,
        ))
        runCurrent()

        assertTrue("Sleep Mode" to "true" in configurationDouble.setFlags, "Sleep mode is set on bedroom door close")

        daemon.cancelAndJoin()
    }

    @Test
    fun autoEnableAfterMidnight() = runTest {
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
            clock = clock.withTimeZone(timeZone),
        )

        val daemon = launch { picker.start() }
        runCurrent()

        fakeEvents.mutableEvents.emit(Event.Latch(
            source = FakeDevices.Latch.id,
            timestamp = Instant.DISTANT_PAST,
            state = LatchState.CLOSED,
        ))
        runCurrent()

        assertTrue("Sleep Mode" to "true" in configurationDouble.setFlags, "Sleep mode is set on bedroom door close")

        daemon.cancelAndJoin()
    }

    @Test
    fun skipEnableBeforeNight() = runTest {
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
            clock = clock.withTimeZone(timeZone),
        )

        val daemon = launch { picker.start() }

        fakeEvents.mutableEvents.emit(Event.Latch(
            source = FakeDevices.Latch.id,
            timestamp = Instant.DISTANT_PAST,
            state = LatchState.CLOSED,
        ))

        assertTrue("Sleep Mode" to "true" !in configurationDouble.setFlags, "Sleep mode not set during day")

        daemon.cancelAndJoin()
    }

    @Test
    fun lightsOffOnEnable() = runTest {
        val fakeConfig = object: ConfigurationAccess by configurationDouble {
            val mutableFlags = MutableSharedFlow<Map<String, String?>>()
            override val flags = mutableFlags.asOngoing()
        }
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = EventAccessStub,
            actionPublisher = actionSpy,
        )
        val picker = SleepMode(client)

        val daemon = launch { picker.start() }
        runCurrent()
        fakeConfig.mutableFlags.emit(mapOf("Sleep Mode" to "true"))
        runCurrent()
        assertEquals(1, actionSpy.actions.size, "Lights Dimmed immediately")
        assertTrue(actionSpy.actions.single() is Action.ColorTemperatureChange)
        assertEquals(FakeDevices.HueGroup.id, actionSpy.actions.single().target)
        advanceTimeBy(31.seconds.inWholeMilliseconds)
        assertEquals(2, actionSpy.actions.size, "Lights Turned off after 30 seconds")
        val offAction = actionSpy.actions[1]
        assertTrue(offAction is Action.Switch)
        assertEquals(SwitchState.OFF, offAction.state)


        daemon.cancelAndJoin()
    }

    @Test
    fun noopOnDisable() = runTest {
        val fakeConfig = object: ConfigurationAccess by configurationDouble {
            val mutableFlags = MutableSharedFlow<Map<String, String?>>()
            override val flags = mutableFlags.asOngoing()
        }
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = EventAccessStub,
            actionPublisher = actionSpy,
        )
        val picker = SleepMode(client)

        val daemon = launch { picker.start() }
        runCurrent()
        fakeConfig.mutableFlags.emit(mapOf("Sleep Mode" to "false"))
        runCurrent()
        assertEquals(0, actionSpy.actions.size, "No action on disable")

        daemon.cancelAndJoin()
    }

    @Test
    fun autoDisable() = runTest {
        val client = DummyClient.copy(
            configurationAccess = configurationDouble,
        )
        val picker = SleepMode(client)
        picker.runCron(Instant.DISTANT_PAST.withZone(TimeZone.UTC))

        assertTrue("Sleep Mode" to "false" in configurationDouble.setFlags, "Sleep mode is set to false on cron run")
    }
}
