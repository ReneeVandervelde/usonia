package usonia.rules.lights

import kotlinx.coroutines.test.runTest
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.foundation.FakeRooms
import usonia.kotlin.OngoingFlow
import usonia.kotlin.ongoingFlowOf
import usonia.server.DummyClient
import kotlin.test.Test
import kotlin.test.assertEquals

class DisableModeTest {
    @Test
    fun enabled() = runTest {
        val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
            override val flags: OngoingFlow<Map<String, String?>> = ongoingFlowOf(mapOf(
                "Disable Lights" to "true"
            ))
        }
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
        )

        assertEquals(LightSettings.Ignore, DisableMode(client).getActiveSettings(FakeRooms.LivingRoom))
    }

    @Test
    fun disabled() = runTest {
        val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
            override val flags: OngoingFlow<Map<String, String?>> = ongoingFlowOf(mapOf(
                "Disable Lights" to "false"
            ))
        }
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
        )

        assertEquals(LightSettings.Unhandled, DisableMode(client).getActiveSettings(FakeRooms.LivingRoom))
    }

    @Test
    fun unspecified() = runTest {
        val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
            override val flags: OngoingFlow<Map<String, String?>> = ongoingFlowOf(emptyMap<String, String>())
        }
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
        )

        assertEquals(LightSettings.Unhandled, DisableMode(client).getActiveSettings(FakeRooms.LivingRoom))
    }
}
