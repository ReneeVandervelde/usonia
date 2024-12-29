package usonia.rules.alerts

import com.inkapplications.coroutines.ongoing.OngoingFlow
import com.inkapplications.coroutines.ongoing.ongoingFlowOf
import kimchi.logger.LogLevel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import usonia.core.state.ActionPublisherSpy
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.foundation.Action
import usonia.foundation.FakeSite
import usonia.foundation.FakeUsers
import usonia.foundation.Site
import usonia.server.DummyClient
import kotlin.test.Test
import kotlin.test.assertEquals

class LogErrorAlertsTest {
    private val config = object: ConfigurationAccess by ConfigurationAccessStub {
        override val flags: OngoingFlow<Map<String, String?>> = ongoingFlowOf(mapOf(
            "Log Alerts" to "true"
        ))
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
        val daemon = launch { LogErrorAlerts.startDaemon() }
        runCurrent()
        LogErrorAlerts.log(LogLevel.ERROR, "Test")
        runCurrent()

        assertEquals(1, actionsSpy.actions.size)

        daemon.cancelAndJoin()
    }

    @Test
    fun flagDisabled() = runTest {
        val actionsSpy = ActionPublisherSpy()
        val disabledFlagConfig = object: ConfigurationAccess by config {
            override val flags: OngoingFlow<Map<String, String?>> = ongoingFlowOf(mapOf(
                "Log Alerts" to "false"
            ))
        }
        val client = DummyClient.copy(
            actionPublisher = actionsSpy,
            configurationAccess = disabledFlagConfig,
        )
        LogErrorAlerts.client.value = client
        val daemon = launch { LogErrorAlerts.startDaemon() }
        runCurrent()
        LogErrorAlerts.log(LogLevel.ERROR, "Test")
        runCurrent()

        assertEquals(0, actionsSpy.actions.size)

        daemon.cancelAndJoin()
    }
}
