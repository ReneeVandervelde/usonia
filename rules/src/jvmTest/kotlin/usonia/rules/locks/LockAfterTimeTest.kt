package usonia.rules.locks

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
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
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class LockAfterTimeTest {
    val testSite = FakeSite.copy(
        rooms = setOf(FakeRooms.LivingRoom.copy(
            devices = setOf(
                FakeDevices.Latch.copy(
                    siblings = setOf(FakeDevices.Lock.id),
                    fixture = Fixture.EntryPoint,
                ),
                FakeDevices.Lock,
            )
        ))
    )

    val testClient = DummyClient.copy(
        configurationAccess = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site: OngoingFlow<Site> = ongoingFlowOf(testSite)
        }
    )

    @Test
    fun lock() = runTest {
        val eventAccess = EventAccessFake()
        val actionPublisher = ActionPublisherSpy()
        val fakeClient = testClient.copy(
            eventAccess = eventAccess,
            actionPublisher = actionPublisher,
        )
        val daemon = LockAfterTime(fakeClient, backgroundScope = this)

        val daemonJob = launch { daemon.start() }
        runCurrent()

        eventAccess.mutableEvents.emit(Event.Latch(
            source = FakeDevices.Latch.id,
            timestamp = Instant.DISTANT_PAST,
            state = LatchState.CLOSED,
        ))
        advanceUntilIdle()

        assertEquals(1, actionPublisher.actions.size)
        val action = actionPublisher.actions.single()
        assertTrue(action is Action.Lock)
        assertEquals(LockState.LOCKED, action.state)

        daemonJob.cancelAndJoin()
    }

    @Test
    fun cancelled() = runTest {
        val eventAccess = EventAccessFake()
        val actionPublisher = ActionPublisherSpy()
        val fakeClient = testClient.copy(
            eventAccess = eventAccess,
            actionPublisher = actionPublisher,
        )
        val daemon = LockAfterTime(fakeClient, backgroundScope = this)

        val daemonJob = launch { daemon.start() }

        eventAccess.mutableEvents.emit(Event.Latch(
            source = FakeDevices.Latch.id,
            timestamp = Instant.DISTANT_PAST,
            state = LatchState.CLOSED,
        ))
        advanceTimeBy(5.minutes.inWholeMilliseconds)
        eventAccess.mutableEvents.emit(Event.Latch(
            source = FakeDevices.Latch.id,
            timestamp = Instant.DISTANT_PAST,
            state = LatchState.OPEN,
        ))
        advanceUntilIdle()

        assertEquals(0, actionPublisher.actions.size)

        daemonJob.cancelAndJoin()
    }

    @Test
    fun noAction() = runTest {
        val eventAccess = EventAccessFake()
        val actionPublisher = ActionPublisherSpy()
        val fakeClient = testClient.copy(
            eventAccess = eventAccess,
            actionPublisher = actionPublisher,
        )
        val daemon = LockAfterTime(fakeClient, backgroundScope = this)

        val daemonJob = launch { daemon.start() }

        eventAccess.mutableEvents.emit(Event.Latch(
            source = FakeDevices.Latch.id,
            timestamp = Instant.DISTANT_PAST,
            state = LatchState.OPEN,
        ))
        advanceUntilIdle()

        assertEquals(0, actionPublisher.actions.size)

        daemonJob.cancelAndJoin()
    }
}
