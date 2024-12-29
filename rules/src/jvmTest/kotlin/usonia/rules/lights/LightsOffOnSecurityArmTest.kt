package usonia.rules.lights

import com.inkapplications.coroutines.ongoing.OngoingFlow
import com.inkapplications.coroutines.ongoing.asOngoing
import com.inkapplications.coroutines.ongoing.ongoingFlowOf
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import usonia.core.state.ActionPublisherSpy
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.foundation.*
import usonia.server.DummyClient
import kotlin.test.assertEquals

class LightsOffOnSecurityArmTest {
    @Test
    fun turnLightsOff() = runTest {
        val securityState = MutableStateFlow(SecurityState.Disarmed)
        val config = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site: OngoingFlow<Site> = ongoingFlowOf(
                FakeSite.copy(
                    rooms = setOf(
                        FakeRooms.LivingRoom.copy(
                            devices = setOf(FakeDevices.HueColorLight),
                        )
                    )
                )
            )
            override val securityState: OngoingFlow<SecurityState> = securityState.asOngoing()
        }
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = config,
            actionPublisher = actionSpy,
        )
        val rule = LightsOffOnSecurityArm(client)

        val daemon = launch { rule.startDaemon() }

        securityState.value = SecurityState.Armed
        runCurrent()

        assertEquals(1, actionSpy.actions.size, "Light should be turned off.")

        daemon.cancelAndJoin()
    }

    @Test
    fun noChangeWhenDisarmed() = runTest {
        val securityState = MutableStateFlow(SecurityState.Armed)
        val config = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site: OngoingFlow<Site> = ongoingFlowOf(
                FakeSite.copy(
                    rooms = setOf(
                        FakeRooms.LivingRoom.copy(
                            devices = setOf(FakeDevices.HueColorLight),
                        )
                    )
                )
            )
            override val securityState: OngoingFlow<SecurityState> = securityState.asOngoing()
        }
        val actionSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            configurationAccess = config,
            actionPublisher = actionSpy,
        )
        val rule = LightsOffOnSecurityArm(client)

        val daemon = launch { rule.startDaemon() }

        securityState.value = SecurityState.Disarmed
        runCurrent()

        assertEquals(0, actionSpy.actions.size, "No actions expected.")

        daemon.cancelAndJoin()
    }
}
