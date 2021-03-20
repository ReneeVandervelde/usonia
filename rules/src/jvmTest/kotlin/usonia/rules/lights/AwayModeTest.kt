package usonia.rules.lights

import kotlinx.coroutines.test.runBlockingTest
import kotlinx.datetime.Instant
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.core.state.EventAccess
import usonia.core.state.EventAccessStub
import usonia.foundation.*
import usonia.kotlin.OngoingFlow
import usonia.kotlin.ongoingFlowOf
import usonia.server.DummyClient
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

class AwayModeTest {
    @Test
    fun default() = runBlockingTest {
        val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site: OngoingFlow<Site> = ongoingFlowOf(FakeSite)
        }
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
        )
        val picker = AwayMode(client)

        val result = picker.getActiveSettings(FakeRooms.LivingRoom)

        assertEquals(LightSettings.Unhandled, result)
    }

    @Test
    fun away() = runBlockingTest {
        val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site: OngoingFlow<Site> = ongoingFlowOf(FakeSite.copy(
                users = setOf(FakeUsers.John)
            ))
        }
        val fakeEvents = object: EventAccess by EventAccessStub {
            override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
                return Event.Presence(
                    source = FakeUsers.John.id,
                    timestamp = Instant.Companion.DISTANT_PAST,
                    state = PresenceState.AWAY,
                ) as T
            }
        }
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = fakeEvents,
        )
        val picker = AwayMode(client)

        val result = picker.getActiveSettings(FakeRooms.LivingRoom)

        assertEquals(LightSettings.Ignore, result)
    }

    @Test
    fun home() = runBlockingTest {
        val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site: OngoingFlow<Site> = ongoingFlowOf(FakeSite.copy(
                users = setOf(FakeUsers.John)
            ))
        }
        val fakeEvents = object: EventAccess by EventAccessStub {
            override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
                return Event.Presence(
                    source = FakeUsers.John.id,
                    timestamp = Instant.Companion.DISTANT_PAST,
                    state = PresenceState.HOME,
                ) as T
            }
        }
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
            eventAccess = fakeEvents,
        )
        val picker = AwayMode(client)

        val result = picker.getActiveSettings(FakeRooms.LivingRoom)

        assertEquals(LightSettings.Unhandled, result)
    }
}
