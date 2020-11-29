package usonia.app.alerts.telegram

import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import usonia.foundation.*
import usonia.state.ActionAccessFake
import usonia.state.ConfigurationAccess
import kotlin.test.Test
import kotlin.test.assertEquals

class TelegramAlertsTest {
    @Test
    fun sendAlert() = runBlockingTest {
        val fakeConfigAccess = object: ConfigurationAccess {
            override val site: Flow<Site> = flowOf(
                FakeSite.copy(
                    users = setOf(
                        FakeUsers.John.copy(
                            parameters = mapOf(
                                "telegram.chat" to "fake-chat"
                            ),
                        )
                    ),
                    parameters = mapOf(
                        "telegram.bot" to "test-bot",
                        "telegram.token" to "test-token",
                    )
                )
            )
        }
        val fakeActions = ActionAccessFake()
        val telegramSpy = TelegramSpy()
        val alerts = TelegramAlerts(
            fakeActions,
            fakeConfigAccess,
            telegramSpy,
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
            override val site: Flow<Site> = flowOf(
                FakeSite.copy(
                    users = setOf(
                        FakeUsers.John.copy(
                            parameters = mapOf(
                                "telegram.chat" to "fake-chat"
                            ),
                        )
                    ),
                    parameters = mapOf(
                        "telegram.bot" to "test-bot",
                        "telegram.token" to "test-token",
                    )
                )
            )
        }
        val fakeActions = ActionAccessFake()
        val telegramSpy = TelegramSpy()
        val alerts = TelegramAlerts(
            fakeActions,
            fakeConfigAccess,
            telegramSpy,
        )

        val job = launch { alerts.start() }

        pauseDispatcher {
            fakeActions.actions.emit(Action.Alert(
                target = Uuid("nobody"),
                message = "test"
            ))
        }

        assertEquals(0, telegramSpy.messages.size)

        job.cancelAndJoin()
    }

    @Test
    fun noUserConfig() = runBlockingTest {
        val fakeConfigAccess = object: ConfigurationAccess {
            override val site: Flow<Site> = flowOf(
                FakeSite.copy(
                    users = setOf(
                        FakeUsers.John
                    ),
                    parameters = mapOf(
                        "telegram.bot" to "test-bot",
                        "telegram.token" to "test-token",
                    )
                )
            )
        }
        val fakeActions = ActionAccessFake()
        val telegramSpy = TelegramSpy()
        val alerts = TelegramAlerts(
            fakeActions,
            fakeConfigAccess,
            telegramSpy,
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
            override val site: Flow<Site> = flowOf(
                FakeSite
            )
        }
        val fakeActions = ActionAccessFake()
        val telegramSpy = TelegramSpy()
        val alerts = TelegramAlerts(
            fakeActions,
            fakeConfigAccess,
            telegramSpy,
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
