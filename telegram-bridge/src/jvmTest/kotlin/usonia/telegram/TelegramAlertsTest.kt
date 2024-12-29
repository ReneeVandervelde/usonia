package usonia.telegram

import com.inkapplications.coroutines.ongoing.OngoingFlow
import com.inkapplications.coroutines.ongoing.ongoingFlowOf
import com.inkapplications.telegram.structures.ChatReference
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import usonia.core.state.ActionAccessFake
import usonia.core.state.ConfigurationAccess
import usonia.core.state.ConfigurationAccessStub
import usonia.foundation.*
import usonia.server.DummyClient
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class TelegramAlertsTest {
    @Test
    fun sendAlert() = runTest {
        val fakeConfigAccess = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site: OngoingFlow<Site> = ongoingFlowOf(
                FakeSite.copy(
                    users = setOf(
                        FakeUsers.John.copy(
                            parameters = mapOf(
                                "telegram.chat" to "123"
                            ),
                        )
                    ),
                    bridges = setOf(
                        FakeBridge.copy(
                            service = "telegram",
                            parameters = mapOf(
                                "bot" to "test-bot",
                                "token" to "test-token",
                            ),
                        )
                    ),
                )
            )
        }
        val fakeActions = ActionAccessFake()
        val client = DummyClient.copy(
            actionAccess = fakeActions,
            configurationAccess = fakeConfigAccess,
        )
        val telegramSpy = MessageSpy()
        val alerts = TelegramAlerts(
            client,
            telegramSpy,
            requestScope = this
        )

        val job = launch { alerts.startDaemon() }
        advanceUntilIdle()

        fakeActions.mutableActions.emit(Action.Alert(
            target = FakeUsers.John.id,
            message = "test"
        ))
        advanceUntilIdle()

        assertEquals(1, telegramSpy.messages.size)
        assertEquals(123L, (telegramSpy.messages.first().chatId as? ChatReference.Id)?.value)
        assertEquals("test", telegramSpy.messages.first().text)

        job.cancelAndJoin()
    }

    @Test
    fun sendAlertWithSticker() = runTest {
        val fakeConfigAccess = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site: OngoingFlow<Site> = ongoingFlowOf(
                FakeSite.copy(
                    users = setOf(
                        FakeUsers.John.copy(
                            parameters = mapOf(
                                "telegram.chat" to "123"
                            ),
                        )
                    ),
                    bridges = setOf(
                        FakeBridge.copy(
                            service = "telegram",
                            parameters = mapOf(
                                "bot" to "test-bot",
                                "token" to "test-token",
                            ),
                        )
                    ),
                )
            )
        }
        val fakeActions = ActionAccessFake()
        val client = DummyClient.copy(
            actionAccess = fakeActions,
            configurationAccess = fakeConfigAccess,
        )
        val telegramSpy = MessageSpy()
        val alerts = TelegramAlerts(
            client,
            telegramSpy,
            requestScope = this
        )

        val job = launch { alerts.startDaemon() }
        advanceUntilIdle()

        fakeActions.mutableActions.emit(Action.Alert(
            target = FakeUsers.John.id,
            message = "test",
            icon = Action.Alert.Icon.Disallowed,
        ))
        advanceUntilIdle()

        assertEquals(1, telegramSpy.messages.size)
        assertEquals(123L, (telegramSpy.messages.first().chatId as? ChatReference.Id)?.value)
        assertEquals("test", telegramSpy.messages.first().text)

        assertEquals(1, telegramSpy.stickers.size)
        assertEquals(123L, (telegramSpy.stickers.first().chatId as? ChatReference.Id)?.value)

        job.cancelAndJoin()
    }

    @Test
    fun unknownUser() = runTest {
        val fakeConfigAccess = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site: OngoingFlow<Site> = ongoingFlowOf(
                FakeSite.copy(
                    users = setOf(
                        FakeUsers.John.copy(
                            parameters = mapOf(
                                "telegram.chat" to "fake-chat"
                            ),
                        )
                    ),
                    bridges = setOf(
                        FakeBridge.copy(
                            service = "telegram",
                            parameters = mapOf(
                                "bot" to "test-bot",
                                "token" to "test-token",
                            ),
                        )
                    ),
                )
            )
        }
        val fakeActions = ActionAccessFake()
        val telegramSpy = MessageSpy()
        val client = DummyClient.copy(
            actionAccess = fakeActions,
            configurationAccess = fakeConfigAccess,
        )
        val alerts = TelegramAlerts(
            client,
            telegramSpy,
            requestScope = this
        )

        val job = launch { alerts.startDaemon() }
        advanceUntilIdle()

        fakeActions.mutableActions.emit(Action.Alert(
            target = Identifier("nobody"),
            message = "test"
        ))
        advanceUntilIdle()

        assertEquals(0, telegramSpy.messages.size)

        job.cancelAndJoin()
    }

    @Test
    fun noUserConfig() = runTest {
        val fakeConfigAccess = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site: OngoingFlow<Site> = ongoingFlowOf(
                FakeSite.copy(
                    users = setOf(
                        FakeUsers.John
                    ),
                    bridges = setOf(
                        FakeBridge.copy(
                            service = "telegram",
                            parameters = mapOf(
                                "bot" to "test-bot",
                                "token" to "test-token",
                            ),
                        )
                    ),
                )
            )
        }
        val fakeActions = ActionAccessFake()
        val telegramSpy = MessageSpy()
        val client = DummyClient.copy(
            actionAccess = fakeActions,
            configurationAccess = fakeConfigAccess,
        )
        val alerts = TelegramAlerts(
            client,
            telegramSpy,
            requestScope = this
        )

        val job = launch { alerts.startDaemon() }
        advanceUntilIdle()

        fakeActions.mutableActions.emit(Action.Alert(
            target = FakeUsers.John.id,
            message = "test"
        ))
        advanceUntilIdle()

        assertEquals(0, telegramSpy.messages.size)

        job.cancelAndJoin()
    }

    @Test
    fun noConfig() = runTest {
        val fakeConfigAccess = object: ConfigurationAccess by ConfigurationAccessStub {
            override val site: OngoingFlow<Site> = ongoingFlowOf(
                FakeSite
            )
        }
        val fakeActions = ActionAccessFake()
        val telegramSpy = MessageSpy()
        val client = DummyClient.copy(
            actionAccess = fakeActions,
            configurationAccess = fakeConfigAccess,
        )
        val alerts = TelegramAlerts(
            client,
            telegramSpy,
            requestScope = this
        )

        val job = launch { alerts.startDaemon() }
        advanceUntilIdle()

        fakeActions.mutableActions.emit(Action.Alert(
            target = FakeUsers.John.id,
            message = "test"
        ))
        advanceUntilIdle()

        assertEquals(0, telegramSpy.messages.size)

        job.cancelAndJoin()
    }
}
