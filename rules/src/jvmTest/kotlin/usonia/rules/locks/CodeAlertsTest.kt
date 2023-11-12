package usonia.rules.locks

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import usonia.core.state.ActionPublisherSpy
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.core.state.EventAccessFake
import usonia.foundation.*
import usonia.kotlin.OngoingFlow
import usonia.kotlin.ongoingFlowOf
import usonia.server.DummyClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CodeAlertsTest {
    private val fakeSite = FakeSite.copy(
        rooms = setOf(FakeRooms.FakeBedroom.copy(
            devices = setOf(FakeDevices.Lock.copy(
                id = Identifier("test-lock"),
                parameters = mapOf(
                    "ownerCodes" to "1,2, 3"
                )
            ))
        )),
        users = setOf(
            FakeUsers.John.copy(
                id = Identifier("test-user"),
                alertLevel = Action.Alert.Level.Debug,
            )
        )
    )
    private val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
        override val site: OngoingFlow<Site> = ongoingFlowOf(fakeSite)
    }

    @Test
    fun guestCode() = runTest {
        val actionSpy = ActionPublisherSpy()
        val fakeEvents = EventAccessFake()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = fakeEvents,
            actionPublisher = actionSpy,
        )
        val alerts = CodeAlerts(client)

        val daemon = launch { alerts.startDaemon() }
        runCurrent()
        fakeEvents.mutableEvents.emit(Event.Lock(
            source = Identifier("test-lock"),
            timestamp = Instant.DISTANT_PAST,
            state = LockState.UNLOCKED,
            method = Event.Lock.LockMethod.KEYPAD,
            code = "420",
        ))
        advanceUntilIdle()

        assertEquals(1, actionSpy.actions.size, "Alert Action Published.")
        val alert = actionSpy.actions.first()
        assertTrue(alert is Action.Alert)
        assertEquals("test-user", alert.target.value)
        assertEquals(Action.Alert.Level.Warning, alert.level)

        daemon.cancelAndJoin()
    }

    @Test
    fun allowedCode() = runTest {
        val actionSpy = ActionPublisherSpy()
        val fakeEvents = EventAccessFake()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = fakeEvents,
            actionPublisher = actionSpy,
        )
        val alerts = CodeAlerts(client)

        val daemon = launch { alerts.startDaemon() }
        runCurrent()
        fakeEvents.mutableEvents.emit(Event.Lock(
            source = Identifier("test-lock"),
            timestamp = Instant.DISTANT_PAST,
            state = LockState.UNLOCKED,
            method = Event.Lock.LockMethod.KEYPAD,
            code = "2",
        ))
        advanceUntilIdle()

        assertEquals(0, actionSpy.actions.size, "No alerts published.")

        daemon.cancelAndJoin()
    }

    @Test
    fun lockEvent() = runTest {
        val actionSpy = ActionPublisherSpy()
        val fakeEvents = EventAccessFake()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = fakeEvents,
            actionPublisher = actionSpy,
        )
        val alerts = CodeAlerts(client)

        val daemon = launch { alerts.startDaemon() }
        runCurrent()
        fakeEvents.mutableEvents.emit(Event.Lock(
            source = Identifier("test-lock"),
            timestamp = Instant.DISTANT_PAST,
            state = LockState.LOCKED,
            method = Event.Lock.LockMethod.KEYPAD,
            code = "420",
        ))
        advanceUntilIdle()

        assertEquals(0, actionSpy.actions.size, "No alerts published.")

        daemon.cancelAndJoin()
    }

    @Test
    fun nonKeypad() = runTest {
        val actionSpy = ActionPublisherSpy()
        val fakeEvents = EventAccessFake()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = fakeEvents,
            actionPublisher = actionSpy,
        )
        val alerts = CodeAlerts(client)

        val daemon = launch { alerts.startDaemon() }
        runCurrent()
        fakeEvents.mutableEvents.emit(Event.Lock(
            source = Identifier("test-lock"),
            timestamp = Instant.DISTANT_PAST,
            state = LockState.UNLOCKED,
            method = Event.Lock.LockMethod.COMMAND,
            code = "420",
        ))
        advanceUntilIdle()

        assertEquals(0, actionSpy.actions.size, "No alerts published.")

        daemon.cancelAndJoin()
    }

    @Test
    fun noCodes() = runTest {
        val site = fakeSite.copy(
            rooms = setOf(FakeRooms.FakeBedroom.copy(
                devices = setOf(FakeDevices.Lock.copy(
                    id = Identifier("test-lock"),
                ))
            )),
        )
        val config = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site: OngoingFlow<Site> = ongoingFlowOf(site)
        }
        val actionSpy = ActionPublisherSpy()
        val fakeEvents = EventAccessFake()
        val client = DummyClient.copy(
            configurationAccess = config,
            eventAccess = fakeEvents,
            actionPublisher = actionSpy,
        )
        val alerts = CodeAlerts(client)

        val daemon = launch { alerts.startDaemon() }
        runCurrent()
        fakeEvents.mutableEvents.emit(Event.Lock(
            source = Identifier("test-lock"),
            timestamp = Instant.DISTANT_PAST,
            state = LockState.UNLOCKED,
            method = Event.Lock.LockMethod.KEYPAD,
            code = "420",
        ))
        advanceUntilIdle()

        assertEquals(0, actionSpy.actions.size, "No alerts published.")

        daemon.cancelAndJoin()
    }
}
