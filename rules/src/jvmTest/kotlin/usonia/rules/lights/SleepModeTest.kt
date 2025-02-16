package usonia.rules.lights

import com.inkapplications.coroutines.ongoing.OngoingFlow
import com.inkapplications.coroutines.ongoing.asOngoing
import com.inkapplications.coroutines.ongoing.ongoingFlowOf
import com.inkapplications.datetime.FixedClock
import com.inkapplications.datetime.atZone
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
import usonia.server.DummyClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class SleepModeTest {
    private val fakeSite = FakeSite.copy(
        rooms = setOf(FakeRooms.FakeBedroom.copy(
            devices = setOf(FakeDevices.Latch, FakeDevices.HueGroup)
        ))
    )

    private val night = FixedClock(LocalDateTime.parse("2020-01-02T23:00:00").atZone(TimeZone.UTC).instant)
    private val morning = FixedClock(LocalDateTime.parse("2020-01-02T05:00:00").atZone(TimeZone.UTC).instant)

    @Test
    fun enabled() = runTest {
        val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site: OngoingFlow<Site> = ongoingFlowOf(fakeSite)
            override val flags: OngoingFlow<Map<String, String?>> = ongoingFlowOf(
                mapOf("Sleep Mode" to "true")
            )
        }
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            actionAccess = ActionAccessFake(),
        )
        val rule = SleepMode(client, clock = night.atZone(TimeZone.UTC))

        val bedroom = rule.getActiveSettings(FakeRooms.FakeBedroom)
        val livingRoom = rule.getActiveSettings(FakeRooms.LivingRoom)
        val tranquilHallwayResult = rule.getActiveSettings(FakeRooms.FakeHallway.copy(
            adjacentRooms = setOf(FakeRooms.FakeBedroom.id)
        ))
        val unaffectedHallwayResult = rule.getActiveSettings(FakeRooms.FakeHallway)
        val bathroom = rule.getActiveSettings(FakeRooms.FakeBathroom)

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
        val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site: OngoingFlow<Site> = ongoingFlowOf(fakeSite)
            override val flags: OngoingFlow<Map<String, String?>> = ongoingFlowOf(
                mapOf("Sleep Mode" to "true")
            )
        }
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            actionAccess = ActionAccessFake(),
        )
        val rule = SleepMode(client, clock = morning.atZone(TimeZone.UTC))

        val bedroom = rule.getActiveSettings(FakeRooms.FakeBedroom)
        val livingRoom = rule.getActiveSettings(FakeRooms.LivingRoom)
        val tranquilHallwayResult = rule.getActiveSettings(FakeRooms.FakeHallway.copy(
            adjacentRooms = setOf(FakeRooms.FakeBedroom.id)
        ))
        val unaffectedHallwayResult = rule.getActiveSettings(FakeRooms.FakeHallway)
        val bathroom = rule.getActiveSettings(FakeRooms.FakeBathroom)

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
        val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site: OngoingFlow<Site> = ongoingFlowOf(fakeSite)
            override val flags: OngoingFlow<Map<String, String?>> = ongoingFlowOf(
                mapOf("Sleep Mode" to "false")
            )
        }
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            actionAccess = ActionAccessFake(),
        )
        val rule = SleepMode(client)

        val bedroom = rule.getActiveSettings(FakeRooms.FakeBedroom)
        val livingRoom = rule.getActiveSettings(FakeRooms.LivingRoom)
        val tranquilHallwayResult = rule.getActiveSettings(FakeRooms.FakeHallway.copy(
            adjacentRooms = setOf(FakeRooms.FakeBedroom.id)
        ))
        val unaffectedHallwayResult = rule.getActiveSettings(FakeRooms.FakeHallway)
        val bathroom = rule.getActiveSettings(FakeRooms.FakeBathroom)

        assertTrue(tranquilHallwayResult is LightSettings.Unhandled)
        assertTrue(bathroom is LightSettings.Unhandled)
        assertTrue(bedroom is LightSettings.Unhandled)
        assertTrue(livingRoom is LightSettings.Unhandled)
        assertTrue(unaffectedHallwayResult is LightSettings.Unhandled)
    }

    @Test
    fun autoEnable() = runTest {
        val timeZone = TimeZone.UTC
        val clock = FixedClock(LocalDateTime(
                year = 2000,
                monthNumber = 1,
                dayOfMonth = 1,
                hour = 23,
                minute = 0,
                second = 0,
                nanosecond = 0,
            ).toInstant(timeZone)
        )
        val spyConfig = object: ConfigurationAccessSpy() {
            override val site: OngoingFlow<Site> = ongoingFlowOf(fakeSite)
        }
        val fakeEvents = EventAccessFake()
        val client = DummyClient.copy(
            configurationAccess = spyConfig,
            eventAccess = fakeEvents,
            actionAccess = ActionAccessFake(),
        )
        val rule = SleepMode(
            client = client,
            clock = clock.atZone(timeZone),
        )

        val daemon = launch { rule.startDaemon() }
        runCurrent()

        fakeEvents.mutableEvents.emit(Event.Latch(
            source = FakeDevices.Latch.id,
            timestamp = Instant.DISTANT_PAST,
            state = LatchState.CLOSED,
        ))
        runCurrent()

        assertTrue("Sleep Mode" to "true" in spyConfig.flagUpdates, "Sleep mode is set on bedroom door close")

        daemon.cancelAndJoin()
    }

    @Test
    fun autoEnableAfterMidnight() = runTest {
        val timeZone = TimeZone.UTC
        val clock = FixedClock(LocalDateTime(
                year = 2000,
                monthNumber = 1,
                dayOfMonth = 1,
                hour = 1,
                minute = 0,
                second = 0,
                nanosecond = 0,
            ).toInstant(timeZone)
        )
        val spyConfig = object: ConfigurationAccessSpy() {
            override val site: OngoingFlow<Site> = ongoingFlowOf(fakeSite)
        }
        val fakeEvents = EventAccessFake()
        val client = DummyClient.copy(
            configurationAccess = spyConfig,
            eventAccess = fakeEvents,
            actionAccess = ActionAccessFake(),
        )
        val rule = SleepMode(
            client = client,
            clock = clock.atZone(timeZone),
        )

        val daemon = launch { rule.startDaemon() }
        runCurrent()

        fakeEvents.mutableEvents.emit(Event.Latch(
            source = FakeDevices.Latch.id,
            timestamp = Instant.DISTANT_PAST,
            state = LatchState.CLOSED,
        ))
        runCurrent()

        assertTrue("Sleep Mode" to "true" in spyConfig.flagUpdates, "Sleep mode is set on bedroom door close")

        daemon.cancelAndJoin()
    }

    @Test
    fun skipEnableBeforeNight() = runTest {
        val timeZone = TimeZone.UTC
        val clock = FixedClock(LocalDateTime(
                year = 2000,
                monthNumber = 1,
                dayOfMonth = 1,
                hour = 8,
                minute = 0,
                second = 0,
                nanosecond = 0,
            ).toInstant(timeZone)
        )
        val spyConfig = object: ConfigurationAccessSpy() {
            override val site: OngoingFlow<Site> = ongoingFlowOf(fakeSite)
        }
        val fakeEvents = EventAccessFake()
        val client = DummyClient.copy(
            configurationAccess = spyConfig,
            eventAccess = fakeEvents,
            actionAccess = ActionAccessFake(),
        )
        val rule = SleepMode(
            client = client,
            clock = clock.atZone(timeZone),
        )

        val daemon = launch { rule.startDaemon() }

        fakeEvents.mutableEvents.emit(Event.Latch(
            source = FakeDevices.Latch.id,
            timestamp = Instant.DISTANT_PAST,
            state = LatchState.CLOSED,
        ))

        assertTrue("Sleep Mode" to "true" !in spyConfig.flagUpdates, "Sleep mode not set during day")

        daemon.cancelAndJoin()
    }

    @Test
    fun lightsOffOnEnable() = runTest {
        val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site: OngoingFlow<Site> = ongoingFlowOf(fakeSite)
            val mutableFlags = MutableSharedFlow<Map<String, String?>>()
            override val flags = mutableFlags.asOngoing()
        }
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = EventAccessStub,
            actionPublisher = actionSpy,
            actionAccess = ActionAccessFake(),
        )
        val rule = SleepMode(client)

        val daemon = launch { rule.startDaemon() }
        runCurrent()
        fakeConfig.mutableFlags.emit(mapOf("Sleep Mode" to "true"))
        runCurrent()
        assertEquals(1, actionSpy.actions.size, "Lights Dimmed immediately")
        assertTrue(actionSpy.actions.single() is Action.ColorTemperatureChange)
        assertEquals(FakeDevices.HueGroup.id, actionSpy.actions.single().target)
        advanceTimeBy(91.seconds.inWholeMilliseconds)
        assertEquals(2, actionSpy.actions.size, "Lights Turned off after 90 seconds")
        val offAction = actionSpy.actions[1]
        assertTrue(offAction is Action.Switch)
        assertEquals(SwitchState.OFF, offAction.state)

        daemon.cancelAndJoin()
    }

    @Test
    fun noopOnDisable() = runTest {
        val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
            val mutableFlags = MutableSharedFlow<Map<String, String?>>()
            override val flags = mutableFlags.asOngoing()
        }
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = EventAccessStub,
            actionPublisher = actionSpy,
            actionAccess = ActionAccessFake(),
        )
        val picker = SleepMode(client)

        val daemon = launch { picker.startDaemon() }
        runCurrent()
        fakeConfig.mutableFlags.emit(mapOf("Sleep Mode" to "false"))
        runCurrent()
        assertEquals(0, actionSpy.actions.size, "No action on disable")

        daemon.cancelAndJoin()
    }

    @Test
    fun intentEnable() = runTest {
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
        val spyConfig = object: ConfigurationAccessSpy() {
            override val site: OngoingFlow<Site> = ongoingFlowOf(fakeSite)
        }
        val actions = ActionAccessFake()
        val client = DummyClient.copy(
            configurationAccess = spyConfig,
            eventAccess = EventAccessStub,
            actionAccess = actions,
        )
        val sleepMode = SleepMode(
            client = client,
            clock = clock.atZone(timeZone),
            backgroundScope = backgroundScope,
        )

        val daemon = launch { sleepMode.startDaemon() }
        runCurrent()
        actions.mutableActions.emit(Action.Intent(
            target = FakeSite.id,
            action = "bed.enter",
        ))
        runCurrent()

        assertEquals("Sleep Mode" to "true", spyConfig.flagUpdates.last())
        daemon.cancelAndJoin()
    }

    @Test
    fun autoDisableByCron() = runTest {
        val spyConfig = object: ConfigurationAccessSpy() {
            override val site: OngoingFlow<Site> = ongoingFlowOf(fakeSite)
        }
        val client = DummyClient.copy(
            configurationAccess = spyConfig,
        )
        val picker = SleepMode(client)
        picker.runCron(Instant.DISTANT_PAST.toLocalDateTime(TimeZone.UTC), TimeZone.UTC)

        assertTrue("Sleep Mode" to "false" in spyConfig.flagUpdates, "Sleep mode is set to false on cron run")
    }

    @Test
    fun autoDisableByLatch() = runTest {
        return@runTest // Temporarily Disabled
        val timeZone = TimeZone.UTC
        val clock = object: Clock {
            override fun now(): Instant = LocalDateTime(
                year = 2000,
                monthNumber = 1,
                dayOfMonth = 1,
                hour = 9,
                minute = 0,
                second = 0,
                nanosecond = 0,
            ).toInstant(timeZone)
        }
        val spyConfig = object: ConfigurationAccessSpy() {
            override val site: OngoingFlow<Site> = ongoingFlowOf(fakeSite)
        }
        val fakeEvents = EventAccessFake()
        val client = DummyClient.copy(
            configurationAccess = spyConfig,
            eventAccess = fakeEvents,
            actionAccess = ActionAccessFake(),
        )
        val sleepMode = SleepMode(
            client = client,
            clock = clock.atZone(timeZone),
            backgroundScope = this,
        )
        val daemon = launch { sleepMode.startDaemon() }
        runCurrent()
        fakeEvents.mutableEvents.emit(Event.Latch(
            source = FakeDevices.Latch.id,
            timestamp = Instant.DISTANT_PAST,
            state = LatchState.OPEN,
        ))
        advanceTimeBy(11.minutes.inWholeMilliseconds)
        runCurrent()

        assertEquals("Sleep Mode" to "false", spyConfig.flagUpdates.last())
        daemon.cancelAndJoin()
    }

    @Test
    fun autoDisableCancel() = runTest {
        val timeZone = TimeZone.UTC
        val clock = object: Clock {
            override fun now(): Instant = LocalDateTime(
                year = 2000,
                monthNumber = 1,
                dayOfMonth = 1,
                hour = 9,
                minute = 0,
                second = 0,
                nanosecond = 0,
            ).toInstant(timeZone)
        }
        val spyConfig = object: ConfigurationAccessSpy() {
            override val site: OngoingFlow<Site> = ongoingFlowOf(fakeSite)
        }
        val fakeEvents = EventAccessFake()
        val client = DummyClient.copy(
            configurationAccess = spyConfig,
            eventAccess = fakeEvents,
            actionAccess = ActionAccessFake(),
        )
        val sleepMode = SleepMode(
            client = client,
            clock = clock.atZone(timeZone),
            backgroundScope = this,
        )
        val daemon = launch { sleepMode.startDaemon() }
        runCurrent()
        fakeEvents.mutableEvents.emit(Event.Latch(
            source = FakeDevices.Latch.id,
            timestamp = Instant.DISTANT_PAST,
            state = LatchState.OPEN,
        ))
        advanceTimeBy(5.minutes.inWholeMilliseconds)
        runCurrent()
        fakeEvents.mutableEvents.emit(Event.Latch(
            source = FakeDevices.Latch.id,
            timestamp = Instant.DISTANT_PAST,
            state = LatchState.CLOSED,
        ))
        advanceTimeBy(6.minutes.inWholeMilliseconds)
        runCurrent()

        assertTrue(spyConfig.flagUpdates.isEmpty(), "No flags changed if door closed before timer")
        daemon.cancelAndJoin()
    }
}
