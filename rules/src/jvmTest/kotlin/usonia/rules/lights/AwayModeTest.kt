package usonia.rules.lights

import kotlinx.coroutines.test.runTest
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.foundation.*
import usonia.kotlin.OngoingFlow
import usonia.kotlin.ongoingFlowOf
import usonia.server.DummyClient
import kotlin.test.Test
import kotlin.test.assertEquals

class AwayModeTest {
    @Test
    fun away() = runTest {
        val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
            override val securityState: OngoingFlow<SecurityState> = ongoingFlowOf(SecurityState.Armed)
        }
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
        )
        val picker = AwayMode(client)

        val result = picker.getActiveSettings(FakeRooms.LivingRoom)

        assertEquals(LightSettings.Ignore, result)
    }

    @Test
    fun home() = runTest {
        val fakeConfig = object: ConfigurationAccess by ConfigurationAccessStub {
            override val securityState: OngoingFlow<SecurityState> = ongoingFlowOf(SecurityState.Disarmed)
        }
        val client = DummyClient.copy(
            configurationAccess = fakeConfig,
        )
        val picker = AwayMode(client)

        val result = picker.getActiveSettings(FakeRooms.LivingRoom)

        assertEquals(LightSettings.Unhandled, result)
    }
}
