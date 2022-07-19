package usonia.rules.charging

import inkapplications.spondee.measures.watts
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PowerLimitChargeTest {
    private val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
        override val site: OngoingFlow<Site> = ongoingFlowOf(FakeSite.copy(
            rooms = setOf(
                FakeRooms.LivingRoom.copy(
                    devices = setOf(FakeDevices.Switch.copy(fixture = Fixture.Charger))
                )
            )
        ))
    }

    @Test
    fun turnsOff() = runTest {
        val actionSpy = ActionPublisherSpy()
        val fakeEvents = EventAccessFake()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = fakeEvents,
            actionPublisher = actionSpy,
        )
        val daemon = PowerLimitCharge(client)

        val daemonJob = launch { daemon.start() }
        runCurrent()

        fakeEvents.mutableEvents.emit(Event.Power(
            source = FakeDevices.Switch.id,
            timestamp = Instant.DISTANT_PAST,
            power = 5.watts,
        ))
        fakeEvents.mutableEvents.emit(Event.Power(
            source = FakeDevices.Switch.id,
            timestamp = Instant.DISTANT_PAST,
            power = 4.watts,
        ))
        fakeEvents.mutableEvents.emit(Event.Power(
            source = FakeDevices.Switch.id,
            timestamp = Instant.DISTANT_PAST,
            power = 3.watts,
        ))
        fakeEvents.mutableEvents.emit(Event.Power(
            source = FakeDevices.Switch.id,
            timestamp = Instant.DISTANT_PAST,
            power = 2.watts,
        ))
        runCurrent()

        val action = actionSpy.actions.singleOrNull()
        assertNotNull(action)
        assertTrue(action is Action.Switch)
        assertEquals(SwitchState.OFF, action.state)

        daemonJob.cancelAndJoin()
    }

    @Test
    fun flat() = runTest {
        val actionSpy = ActionPublisherSpy()
        val fakeEvents = EventAccessFake()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = fakeEvents,
            actionPublisher = actionSpy,
        )
        val daemon = PowerLimitCharge(client)

        val daemonJob = launch { daemon.start() }
        fakeEvents.mutableEvents.emit(Event.Power(
            source = FakeDevices.Switch.id,
            timestamp = Instant.DISTANT_PAST,
            power = 5.watts,
        ))
        fakeEvents.mutableEvents.emit(Event.Power(
            source = FakeDevices.Switch.id,
            timestamp = Instant.DISTANT_PAST,
            power = 4.watts,
        ))
        fakeEvents.mutableEvents.emit(Event.Power(
            source = FakeDevices.Switch.id,
            timestamp = Instant.DISTANT_PAST,
            power = 4.watts,
        ))
        fakeEvents.mutableEvents.emit(Event.Power(
            source = FakeDevices.Switch.id,
            timestamp = Instant.DISTANT_PAST,
            power = 2.watts,
        ))
        runCurrent()

        assertTrue(actionSpy.actions.isEmpty())

        daemonJob.cancelAndJoin()
    }

    @Test
    fun rising() = runTest {
        val actionSpy = ActionPublisherSpy()
        val fakeEvents = EventAccessFake()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = fakeEvents,
            actionPublisher = actionSpy,
        )
        val daemon = PowerLimitCharge(client)

        val daemonJob = launch { daemon.start() }
        fakeEvents.mutableEvents.emit(Event.Power(
            source = FakeDevices.Switch.id,
            timestamp = Instant.DISTANT_PAST,
            power = 5.watts,
        ))
        fakeEvents.mutableEvents.emit(Event.Power(
            source = FakeDevices.Switch.id,
            timestamp = Instant.DISTANT_PAST,
            power = 6.watts,
        ))
        fakeEvents.mutableEvents.emit(Event.Power(
            source = FakeDevices.Switch.id,
            timestamp = Instant.DISTANT_PAST,
            power = 7.watts,
        ))
        fakeEvents.mutableEvents.emit(Event.Power(
            source = FakeDevices.Switch.id,
            timestamp = Instant.DISTANT_PAST,
            power = 8.watts,
        ))
        runCurrent()

        assertTrue(actionSpy.actions.isEmpty())

        daemonJob.cancelAndJoin()
    }

    @Test
    fun mixed() = runTest {
        val actionSpy = ActionPublisherSpy()
        val fakeEvents = EventAccessFake()
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = fakeEvents,
            actionPublisher = actionSpy,
        )
        val daemon = PowerLimitCharge(client)

        val daemonJob = launch { daemon.start() }
        fakeEvents.mutableEvents.emit(Event.Power(
            source = FakeDevices.Switch.id,
            timestamp = Instant.DISTANT_PAST,
            power = 5.watts,
        ))
        fakeEvents.mutableEvents.emit(Event.Power(
            source = FakeDevices.Switch.id,
            timestamp = Instant.DISTANT_PAST,
            power = 4.watts,
        ))
        fakeEvents.mutableEvents.emit(Event.Power(
            source = FakeDevices.Switch.id,
            timestamp = Instant.DISTANT_PAST,
            power = 5.watts,
        ))
        fakeEvents.mutableEvents.emit(Event.Power(
            source = FakeDevices.Switch.id,
            timestamp = Instant.DISTANT_PAST,
            power = 4.watts,
        ))
        runCurrent()

        assertTrue(actionSpy.actions.isEmpty())

        daemonJob.cancelAndJoin()
    }
}
