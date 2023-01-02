package usonia.hue

import inkapplications.shade.events.Events
import inkapplications.shade.groupedlights.events.GroupedLightEvent
import inkapplications.shade.lights.events.LightEvent
import inkapplications.shade.structures.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.core.state.EventPublisher
import usonia.core.state.EventPublisherSpy
import usonia.foundation.*
import usonia.kotlin.OngoingFlow
import usonia.kotlin.ongoingFlowOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(UndocumentedApi::class, ExperimentalCoroutinesApi::class)
class HueEventPublisherTest {
    private val hueEvents = object: Events {
        override fun bridgeEvents(): Flow<List<Any>> = flowOf(
            listOf(
                LightEvent(
                    id = ResourceId("FAKE-LIGHT-ID"),
                    owner = ResourceReference(ResourceId("TEST-OWNER"), ResourceType.Light),
                    powerInfo = PowerInfo(on = true),
                ),
                GroupedLightEvent(
                    id = ResourceId("FAKE-GROUP-ID"),
                    owner = ResourceReference(ResourceId("TEST-OWNER"), ResourceType.Light),
                    powerInfo = PowerInfo(on = false),
                )
            ),
        )
    }

    private val configuration = object: ConfigurationAccess by ConfigurationAccessStub {
        override val site: OngoingFlow<Site> = ongoingFlowOf(
            FakeSite.copy(
                bridges = setOf(
                    FakeBridge.copy(
                        service = HUE_SERVICE,
                    )
                ),
                rooms = setOf(
                    FakeRooms.LivingRoom.copy(
                        devices = setOf(
                            FakeDevices.HueColorLight.copy(
                                parent = ExternalAssociation(
                                    context = FakeBridge.id,
                                    id = Identifier("FAKE-LIGHT-ID"),
                                ),
                            ),
                            FakeDevices.HueGroup.copy(
                                parent = ExternalAssociation(
                                    context = FakeBridge.id,
                                    id = Identifier("FAKE-GROUP-ID"),
                                ),
                            )
                        )
                    )
                )
            )
        )
    }

    val clock = object: Clock {
        override fun now(): Instant = Instant.fromEpochSeconds(42069)
    }

    @Test
    fun publishSwitchState() = runTest {
        val publisherSpy = EventPublisherSpy()
        val publisher = HueEventPublisher(hueEvents, configuration, publisherSpy, clock)

        val daemon = launch { publisher.start() }
        advanceUntilIdle()

        assertEquals(2, publisherSpy.events.size, "Two events should be published.")

        assertEquals(FakeDevices.HueColorLight.id, publisherSpy.events[0].source)
        assertEquals(42069, publisherSpy.events[0].timestamp.epochSeconds)
        assertEquals(SwitchState.ON, (publisherSpy.events[0] as? Event.Switch)?.state)

        assertEquals(FakeDevices.HueGroup.id, publisherSpy.events[1].source)
        assertEquals(42069, publisherSpy.events[1].timestamp.epochSeconds)
        assertEquals(SwitchState.OFF, (publisherSpy.events[1] as? Event.Switch)?.state)

        daemon.cancel()
    }

    @Test
    fun noPowerInfo() = runTest {
        val publisherSpy = EventPublisherSpy()
        val hueEvents = object: Events {
            override fun bridgeEvents(): Flow<List<Any>> = flowOf(
                listOf(
                    LightEvent(
                        id = ResourceId("FAKE-LIGHT-ID"),
                        owner = ResourceReference(ResourceId("TEST-OWNER"), ResourceType.Light),
                    ),
                    GroupedLightEvent(
                        id = ResourceId("FAKE-GROUP-ID"),
                        owner = ResourceReference(ResourceId("TEST-OWNER"), ResourceType.Light),
                    )
                ),
            )
        }
        val publisher = HueEventPublisher(hueEvents, configuration, publisherSpy, clock)

        val daemon = launch { publisher.start() }
        advanceUntilIdle()

        assertTrue(publisherSpy.events.none { it is Event.Switch }, "Switch state not published without power info.")

        daemon.cancel()
    }

    @Test
    fun noEvents() = runTest {
        val publisherSpy = EventPublisherSpy()
        val hueEvents = object: Events {
            override fun bridgeEvents(): Flow<List<Any>> = flowOf()
        }
        val publisher = HueEventPublisher(hueEvents, configuration, publisherSpy, clock)

        val daemon = launch { publisher.start() }
        advanceUntilIdle()

        assertEquals(0, publisherSpy.events.size, "No events published without hue events.")

        daemon.cancel()
    }

    @Test
    fun noSiteConfig() = runTest {
        val publisherSpy = EventPublisherSpy()
        val configuration = ConfigurationAccessStub
        val publisher = HueEventPublisher(hueEvents, configuration, publisherSpy, clock)

        val daemon = launch { publisher.start() }
        advanceUntilIdle()

        assertEquals(0, publisherSpy.events.size, "No events published without site config.")

        daemon.cancel()
    }
}
