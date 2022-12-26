package usonia.rules.lights

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.foundation.FakeRooms
import usonia.kotlin.OngoingFlow
import usonia.kotlin.ongoingFlowOf
import usonia.server.DummyClient
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class LightingEnabledFlagTest {
    @Test
    fun enabled() = runTest {
        val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
            override val flags: OngoingFlow<Map<String, String?>> = ongoingFlowOf(mapOf(
                "Motion Lighting" to "true"
            ))
        }
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
        )

        assertEquals(LightSettings.Unhandled, LightingEnabledFlag(client).getActiveSettings(FakeRooms.LivingRoom))
    }

    @Test
    fun disabled() = runTest {
        val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
            override val flags: OngoingFlow<Map<String, String?>> = ongoingFlowOf(mapOf(
                "Motion Lighting" to "false"
            ))
        }
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
        )

        assertEquals(LightSettings.Ignore, LightingEnabledFlag(client).getActiveSettings(FakeRooms.LivingRoom))
    }

    @Test
    fun unspecified() = runTest {
        val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
            override val flags: OngoingFlow<Map<String, String?>> = ongoingFlowOf(emptyMap<String, String>())
        }
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
        )

        assertEquals(LightSettings.Unhandled, LightingEnabledFlag(client).getActiveSettings(FakeRooms.LivingRoom))
    }
}
