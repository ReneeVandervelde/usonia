package usonia.rules.locks

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import usonia.core.state.*
import usonia.foundation.*
import usonia.kotlin.OngoingFlow
import usonia.kotlin.ongoingFlowOf
import usonia.server.DummyClient
import kotlin.test.Test
import kotlin.test.assertEquals

class DisarmOnPrimaryCodeTest {
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
        val fakeEvents = EventAccessFake()
        val securityAccess = FullSecurityAccessSpy()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = fakeEvents,
            fullSecurityAccess = securityAccess,
        )
        val disarmRule = DisarmOnPrimaryCode(client)

        val daemon = launch { disarmRule.startDaemon() }
        runCurrent()
        fakeEvents.mutableEvents.emit(Event.Lock(
            source = Identifier("test-lock"),
            timestamp = Instant.DISTANT_PAST,
            state = LockState.UNLOCKED,
            method = Event.Lock.LockMethod.KEYPAD,
            code = "420",
        ))
        advanceUntilIdle()

        assertEquals(0, securityAccess.disarms.size, "No Disarms made for guest code.")

        daemon.cancelAndJoin()
    }

    @Test
    fun allowedCode() = runTest {
        val fakeEvents = EventAccessFake()
        val securityAccess = FullSecurityAccessSpy()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = fakeEvents,
            fullSecurityAccess = securityAccess,
        )
        val disarmRule = DisarmOnPrimaryCode(client)

        val daemon = launch { disarmRule.startDaemon() }
        runCurrent()
        fakeEvents.mutableEvents.emit(Event.Lock(
            source = Identifier("test-lock"),
            timestamp = Instant.DISTANT_PAST,
            state = LockState.UNLOCKED,
            method = Event.Lock.LockMethod.KEYPAD,
            code = "2",
        ))
        advanceUntilIdle()

        assertEquals(1, securityAccess.disarms.size, "Disarm sent for allowed code.")

        daemon.cancelAndJoin()
    }

    @Test
    fun lockEvent() = runTest {
        val fakeEvents = EventAccessFake()
        val securityAccess = FullSecurityAccessSpy()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = fakeEvents,
            fullSecurityAccess = securityAccess,
        )
        val disarmRule = DisarmOnPrimaryCode(client)

        val daemon = launch { disarmRule.startDaemon() }
        runCurrent()
        fakeEvents.mutableEvents.emit(Event.Lock(
            source = Identifier("test-lock"),
            timestamp = Instant.DISTANT_PAST,
            state = LockState.LOCKED,
            method = Event.Lock.LockMethod.KEYPAD,
            code = "420",
        ))
        advanceUntilIdle()

        assertEquals(0, securityAccess.disarms.size, "No Disarms made for lock events.")

        daemon.cancelAndJoin()
    }

    @Test
    fun nonKeypad() = runTest {
        val fakeEvents = EventAccessFake()
        val securityAccess = FullSecurityAccessSpy()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = fakeEvents,
            fullSecurityAccess = securityAccess,
        )
        val disarmRule = DisarmOnPrimaryCode(client)

        val daemon = launch { disarmRule.startDaemon() }
        runCurrent()
        fakeEvents.mutableEvents.emit(Event.Lock(
            source = Identifier("test-lock"),
            timestamp = Instant.DISTANT_PAST,
            state = LockState.UNLOCKED,
            method = Event.Lock.LockMethod.COMMAND,
            code = "420",
        ))
        advanceUntilIdle()

        assertEquals(0, securityAccess.disarms.size, "No Disarms made for non-keypad unlocks.")

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
        val fakeEvents = EventAccessFake()
        val securityAccess = FullSecurityAccessSpy()
        val client = DummyClient.copy(
            configurationAccess = config,
            eventAccess = fakeEvents,
            fullSecurityAccess = securityAccess,
        )
        val disarmRule = DisarmOnPrimaryCode(client)

        val daemon = launch { disarmRule.startDaemon() }
        runCurrent()
        fakeEvents.mutableEvents.emit(Event.Lock(
            source = Identifier("test-lock"),
            timestamp = Instant.DISTANT_PAST,
            state = LockState.UNLOCKED,
            method = Event.Lock.LockMethod.KEYPAD,
            code = "420",
        ))
        advanceUntilIdle()

        assertEquals(0, securityAccess.disarms.size, "No Disarms made if no codes defined.")

        daemon.cancelAndJoin()
    }
}
