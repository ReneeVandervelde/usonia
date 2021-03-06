package usonia.rules.lights

import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import usonia.core.state.ActionPublisherSpy
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.foundation.FakeRooms
import usonia.server.DummyClient
import kotlin.test.Test
import kotlin.test.assertEquals

class DisableModeTest {
    @Test
    fun enabled() = runBlockingTest {
        val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
            override val flags = flowOf(mapOf(
                "Disable Lights" to "true"
            ))
        }
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
        )

        assertEquals(LightSettings.Ignore, DisableMode(client).getRoomSettings(FakeRooms.LivingRoom))
    }

    @Test
    fun disabled() = runBlockingTest {
        val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
            override val flags = flowOf(mapOf(
                "Disable Lights" to "false"
            ))
        }
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
        )

        assertEquals(LightSettings.Unhandled, DisableMode(client).getRoomSettings(FakeRooms.LivingRoom))
    }

    @Test
    fun unspecified() = runBlockingTest {
        val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
            override val flags = flowOf(emptyMap<String, String>())
        }
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
        )

        assertEquals(LightSettings.Unhandled, DisableMode(client).getRoomSettings(FakeRooms.LivingRoom))
    }
}
