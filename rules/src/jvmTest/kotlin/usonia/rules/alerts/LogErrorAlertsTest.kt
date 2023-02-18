package usonia.rules.alerts

import kimchi.logger.LogLevel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import usonia.core.state.ActionPublisherSpy
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.foundation.*
import usonia.kotlin.OngoingFlow
import usonia.kotlin.ongoingFlowOf
import usonia.server.DummyClient
import kotlin.test.Test
import kotlin.test.assertEquals

class LogErrorAlertsTest {
    private val config = object: ConfigurationAccess by ConfigurationAccessStub {
        override val site: OngoingFlow<Site> = ongoingFlowOf(FakeSite.copy(
            users = setOf(
                FakeUsers.John.copy(alertLevel = Action.Alert.Level.Debug),
                FakeUsers.Jane.copy(alertLevel = Action.Alert.Level.Info),
            ),
        ))
    }

    @Test
    fun sendAlert() = runTest {
        val actionsSpy = ActionPublisherSpy()
        val client = DummyClient.copy(
            actionPublisher = actionsSpy,
            configurationAccess = config,
        )
        LogErrorAlerts.client.value = client
        val daemon = launch { LogErrorAlerts.start() }
        runCurrent()
        LogErrorAlerts.log(LogLevel.ERROR, "Test")
        runCurrent()

        assertEquals(1, actionsSpy.actions.size)

        daemon.cancelAndJoin()
    }
}
