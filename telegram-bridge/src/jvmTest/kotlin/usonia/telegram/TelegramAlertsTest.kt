package usonia.telegram

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import usonia.foundation.*
import usonia.kotlin.suspendedFlow
import usonia.core.state.ActionAccessFake
import usonia.core.state.ConfigurationAccess
import usonia.server.DummyClient
import kotlin.test.Test
import kotlin.test.assertEquals

class TelegramAlertsTest {
    @Test
    fun sendAlert() = runBlockingTest {
        val fakeConfigAccess = object: ConfigurationAccess {
            override val site: Flow<Site> = suspendedFlow(
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
        val client = DummyClient.copy(
            actionAccess = fakeActions,
            configurationAccess = fakeConfigAccess,
        )
        val telegramSpy = TelegramSpy()
        val alerts = TelegramAlerts(
            client,
            telegramSpy,
            requestScope = this
        )

        val job = launch { alerts.start() }

        pauseDispatcher {
            fakeActions.actions.emit(Action.Alert(
                target = FakeUsers.John.id,
                message = "test"
            ))
        }

        assertEquals(1, telegramSpy.messages.size)
        assertEquals("test", telegramSpy.messages.first())

        job.cancelAndJoin()
    }
    @Test
    fun unknownUser() = runBlockingTest {
        val fakeConfigAccess = object: ConfigurationAccess {
            override val site: Flow<Site> = suspendedFlow(
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
        val telegramSpy = TelegramSpy()
        val client = DummyClient.copy(
            actionAccess = fakeActions,
            configurationAccess = fakeConfigAccess,
        )
        val alerts = TelegramAlerts(
            client,
            telegramSpy,
            requestScope = this
        )

        val job = launch { alerts.start() }

        pauseDispatcher {
            fakeActions.actions.emit(Action.Alert(
                target = Identifier("nobody"),
                message = "test"
            ))
        }

        assertEquals(0, telegramSpy.messages.size)

        job.cancelAndJoin()
    }

    @Test
    fun noUserConfig() = runBlockingTest {
        val fakeConfigAccess = object: ConfigurationAccess {
            override val site: Flow<Site> = suspendedFlow(
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
        val telegramSpy = TelegramSpy()
        val client = DummyClient.copy(
            actionAccess = fakeActions,
            configurationAccess = fakeConfigAccess,
        )
        val alerts = TelegramAlerts(
            client,
            telegramSpy,
            requestScope = this
        )

        val job = launch { alerts.start() }

        pauseDispatcher {
            fakeActions.actions.emit(Action.Alert(
                target = FakeUsers.John.id,
                message = "test"
            ))
        }

        assertEquals(0, telegramSpy.messages.size)

        job.cancelAndJoin()
    }

    @Test
    fun noConfig() = runBlockingTest {
        val fakeConfigAccess = object: ConfigurationAccess {
            override val site: Flow<Site> = suspendedFlow(
                FakeSite
            )
        }
        val fakeActions = ActionAccessFake()
        val telegramSpy = TelegramSpy()
        val client = DummyClient.copy(
            actionAccess = fakeActions,
            configurationAccess = fakeConfigAccess,
        )
        val alerts = TelegramAlerts(
            client,
            telegramSpy,
            requestScope = this
        )

        val job = launch { alerts.start() }

        pauseDispatcher {
            fakeActions.actions.emit(Action.Alert(
                target = FakeUsers.John.id,
                message = "test"
            ))
        }

        assertEquals(0, telegramSpy.messages.size)

        job.cancelAndJoin()
    }

    class TelegramSpy: TelegramApi {
        var messages = mutableListOf<String>()
        override suspend fun sendMessage(bot: String, token: String, chatId: String, message: String) {
            messages.add(message)
        }
    }
}
