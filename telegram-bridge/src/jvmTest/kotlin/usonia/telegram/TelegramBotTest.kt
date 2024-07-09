package usonia.telegram

import com.inkapplications.telegram.client.TelegramBotClient
import com.inkapplications.telegram.structures.*
import kimchi.logger.EmptyLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import org.junit.Test
import usonia.core.state.ActionPublisherSpy
import usonia.core.state.ConfigurationAccess
import usonia.core.state.EventPublisherSpy
import usonia.foundation.*
import usonia.kotlin.OngoingFlow
import usonia.kotlin.ongoingFlowOf
import usonia.rules.Flags
import usonia.server.DummyClient
import usonia.server.client.BackendClient
import usonia.server.http.HttpRequest
import usonia.server.http.RestResponse
import usonia.telegram.commands.*
import usonia.telegram.commands.Command
import usonia.telegram.commands.MovieStartCommand
import usonia.telegram.commands.SleepCommand
import usonia.telegram.commands.WakeCommand
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TelegramBotTest {
    @Test
    fun unhandled() = runTest {
        val spy = MessageSpy()
        val bot = TelegramBot(
            client = DummyClient,
            telegram = spy,
            json = Json,
            commands = emptySet(),
            logger = EmptyLogger,
        )
        val response = bot.getResponse(
            Update.EditedMessageUpdate(
                id = 0L,
                message = Message(ChatReference.Id(0L), Instant.DISTANT_PAST, Chat(ChatReference.Id(0L), ChatType.Private)),
            ),
            HttpRequest(headers = emptyMap(), parameters = emptyMap()),
        )

        successfulResponse(response)
        assertEquals(0, spy.messages.size, "No messages sent on unknown update type")
        assertEquals(0, spy.stickers.size, "No stickers sent on unknown update type")
    }

    @Test
    fun unknownCommand() = runTest {
        val messageSpy = MessageSpy()
        val actionSpy = ActionPublisherSpy()
        val setFlags = mutableListOf<Pair<String, String?>>()
        val bot = TelegramBot(
            client = DummyClient.copy(
                configurationAccess = object: ConfigurationAccess by DummyClient {
                    override val site: OngoingFlow<Site> = ongoingFlowOf(FakeSite.copy(
                        users = setOf(FakeUsers.John.copy(
                            alertLevel = Action.Alert.Level.Debug,
                            parameters = mapOf(
                                CHAT_ID_KEY to "123"
                            )
                        ))
                    ))
                    override suspend fun setFlag(key: String, value: String?) {
                        setFlags.add(key to value)
                    }
                },
                actionPublisher = actionSpy,
            ),
            telegram = messageSpy,
            json = Json,
            commands = emptySet(),
            logger = EmptyLogger,
        )
        val response = bot.getResponse(
            Update.MessageUpdate(
                id = 0L,
                message = Message(
                    id = ChatReference.Id(0L),
                    date = Instant.DISTANT_PAST,
                    chat = Chat(ChatReference.Id(123L), ChatType.Private),
                    text = "/unknown",
                    entities = listOf(MessageEntity(MessageEntityType.BotCommand, 0, 8)),
                ),
            ),
            HttpRequest(headers = emptyMap(), parameters = emptyMap()),
        )

        successfulResponse(response)
        assertEquals(1, messageSpy.messages.size, "Single message sent to caller")
        assertEquals(123L, (messageSpy.messages.single().chatId as ChatReference.Id).value)

        assertEquals(1, messageSpy.stickers.size, "Single sticker sent to caller")
        assertEquals(123L, (messageSpy.stickers.single().chatId as ChatReference.Id).value)

        assertEquals(0, setFlags.size, "No flags are changed")
        assertEquals(0, actionSpy.actions.size, "No alerts should be sent")
    }

    @Test
    fun unknownUser() = runTest {
        val messageSpy = MessageSpy()
        val actionSpy = ActionPublisherSpy()
        val bot = TelegramBot(
            client = DummyClient.copy(
                configurationAccess = object: ConfigurationAccess by DummyClient {
                    override val site: OngoingFlow<Site> = ongoingFlowOf(FakeSite.copy(
                        users = setOf(FakeUsers.John.copy(
                            alertLevel = Action.Alert.Level.Debug,
                            parameters = mapOf(
                                CHAT_ID_KEY to "123"
                            )
                        ))
                    ))
                },
                actionPublisher = actionSpy,
            ),
            telegram = messageSpy,
            json = Json,
            commands = emptySet(),
            logger = EmptyLogger,
        )
        val response = bot.getResponse(
            Update.MessageUpdate(
                id = 0L,
                message = Message(ChatReference.Id(0L), Instant.DISTANT_PAST, Chat(ChatReference.Id(543L), ChatType.Private)),
            ),
            HttpRequest(headers = emptyMap(), parameters = emptyMap()),
        )

        successfulResponse(response)
        assertEquals(1, messageSpy.messages.size, "Single message sent to caller")
        assertEquals(543L, (messageSpy.messages.single().chatId as ChatReference.Id).value)

        assertEquals(1, messageSpy.stickers.size, "Single sticker sent to caller")
        assertEquals(543L, (messageSpy.stickers.single().chatId as ChatReference.Id).value)

        assertEquals(1, actionSpy.actions.size, "Alert is sent to admin about unknown user")
        assertEquals(FakeUsers.John.id, actionSpy.actions.single().target)
    }

    @Test
    fun commands() {
        testCommand(::WakeCommand) {
            assertEquals(1, messageSpy.messages.size, "Single message sent to caller")
            assertEquals(123L, (messageSpy.messages.single().chatId as ChatReference.Id).value)

            assertEquals(1, messageSpy.stickers.size, "Single sticker sent to caller")
            assertEquals(123L, (messageSpy.stickers.single().chatId as ChatReference.Id).value)

            assertEquals(1, setFlags.size, "Only sleep mode flag is changed")
            assertEquals(Flags.SleepMode to "false", setFlags.single(), "Sleep mode is disabled")

            assertEquals(0, actionSpy.actions.size, "No alerts should be sent")
        }
        testCommand(::SleepCommand) {
            assertEquals(1, messageSpy.messages.size, "Single message sent to caller")
            assertEquals(123L, (messageSpy.messages.single().chatId as ChatReference.Id).value)

            assertEquals(1, messageSpy.stickers.size, "Single sticker sent to caller")
            assertEquals(123L, (messageSpy.stickers.single().chatId as ChatReference.Id).value)

            assertEquals(1, setFlags.size, "Only sleep mode flag is changed")
            assertEquals(Flags.SleepMode to "true", setFlags.single(), "Sleep mode is enabled")

            assertEquals(0, actionSpy.actions.size, "No alerts should be sent")
        }
        testCommand(::MovieStartCommand) {
            assertEquals(1, messageSpy.messages.size, "Single message sent to caller")
            assertEquals(123L, (messageSpy.messages.single().chatId as ChatReference.Id).value)

            assertEquals(1, messageSpy.stickers.size, "Single sticker sent to caller")
            assertEquals(123L, (messageSpy.stickers.single().chatId as ChatReference.Id).value)

            assertEquals(1, setFlags.size, "Only movie mode flag is changed")
            assertEquals(Flags.MovieMode to "true", setFlags.single(), "Movie mode is enabled")

            assertEquals(0, actionSpy.actions.size, "No alerts should be sent")
        }
        testCommand(::MovieEndCommand) {
            assertEquals(1, messageSpy.messages.size, "Single message sent to caller")
            assertEquals(123L, (messageSpy.messages.single().chatId as ChatReference.Id).value)

            assertEquals(0, messageSpy.stickers.size, "No sticker sent to caller")

            assertEquals(1, setFlags.size, "Only movie mode flag is changed")
            assertEquals(Flags.MovieMode to "false", setFlags.single(), "Movie mode is disabled")

            assertEquals(0, actionSpy.actions.size, "No alerts should be sent")
        }
        testCommand(::PauseLightsCommand) {
            assertEquals(1, messageSpy.messages.size, "Single message sent to caller")
            assertEquals(123L, (messageSpy.messages.single().chatId as ChatReference.Id).value)

            assertEquals(0, messageSpy.stickers.size, "No sticker sent to caller")

            assertEquals(1, setFlags.size, "Only motion lights flag is changed")
            assertEquals(Flags.MotionLights to "false", setFlags.single(), "Motion lights disabled")

            assertEquals(0, actionSpy.actions.size, "No alerts should be sent")
        }
        testCommand(::ResumeLightsCommand) {
            assertEquals(1, messageSpy.messages.size, "Single message sent to caller")
            assertEquals(123L, (messageSpy.messages.single().chatId as ChatReference.Id).value)

            assertEquals(1, messageSpy.stickers.size, "Single sticker sent to caller")
            assertEquals(123L, (messageSpy.stickers.single().chatId as ChatReference.Id).value)

            assertEquals(1, setFlags.size, "Only motion lights flag is changed")
            assertEquals(Flags.MotionLights to "true", setFlags.single(), "Motion lights enabled")

            assertEquals(0, actionSpy.actions.size, "No alerts should be sent")
        }
        testCommand(::AnnounceCommand, text = "Test?") {
            assertEquals(2, actionSpy.actions.size, "Alert sent to all info users")
            assertEquals(listOf(FakeUsers.John.id, FakeUsers.Jane.id), actionSpy.actions.map { (it as? Action.Alert)?.target })
            assertEquals("Test?", (actionSpy.actions.first() as Action.Alert).message)

            assertEquals(0, setFlags.size, "No flags are changed")
        }
        testCommand(::HomeCommand) {
            assertEquals(1, eventSpy.events.size, "Presence event published")
            val event = eventSpy.events.single()
            assertTrue(event is Event.Presence)
            assertEquals(PresenceState.HOME, event.state)
            assertEquals(FakeUsers.John.id, event.source)
        }
        testCommand(::AwayCommand) {
            assertEquals(1, eventSpy.events.size, "Presence event published")
            val event = eventSpy.events.single()
            assertTrue(event is Event.Presence)
            assertEquals(PresenceState.AWAY, event.state)
            assertEquals(FakeUsers.John.id, event.source)
        }
        testCommand(::DisableAlertsCommand) {
            assertEquals(1, setFlags.size, "Flag updated")
            assertEquals(Flags.LogAlerts to "false", setFlags.single())
        }
        testCommand(::EnableAlertsCommand) {
            assertEquals(1, setFlags.size, "Flag updated")
            assertEquals(Flags.LogAlerts to "true", setFlags.single())
        }
    }

    private data class TestCommandContext(
        val actionSpy: ActionPublisherSpy,
        val eventSpy: EventPublisherSpy,
        val messageSpy: MessageSpy,
        val setFlags: List<Pair<String, String?>>,
    )

    private fun testCommand(
        createCommand: (TelegramBotClient, BackendClient) -> Command,
        text: String? = null,
        assertions: TestCommandContext.() -> Unit
    ) = runTest {
        val messageSpy = MessageSpy()
        val actionSpy = ActionPublisherSpy()
        val eventSpy = EventPublisherSpy()
        val setFlags = mutableListOf<Pair<String, String?>>()
        val client = DummyClient.copy(
            configurationAccess = object: ConfigurationAccess by DummyClient {
                override suspend fun setFlag(key: String, value: String?) {
                    setFlags.add(key to value)
                }
                override val site: OngoingFlow<Site> = ongoingFlowOf(FakeSite.copy(
                    users = setOf(
                        FakeUsers.John.copy(
                            alertLevel = Action.Alert.Level.Debug,
                            parameters = mapOf(
                                CHAT_ID_KEY to "123"
                            )
                        ),
                        FakeUsers.Jane.copy(
                            alertLevel = Action.Alert.Level.Info,
                            parameters = mapOf(
                                CHAT_ID_KEY to "456"
                            )
                        )
                    )
                ))
            },
            actionPublisher = actionSpy,
            eventPublisher = eventSpy,
        )
        val command = createCommand(messageSpy, client)
        val bot = TelegramBot(
            client = client,
            telegram = messageSpy,
            json = Json,
            commands = setOf(command),
            logger = EmptyLogger,
        )
        val response = bot.getResponse(
            Update.MessageUpdate(
                id = 0L,
                message = Message(
                    id = ChatReference.Id(0L),
                    date = Instant.DISTANT_PAST,
                    chat = Chat(ChatReference.Id(123L), ChatType.Private),
                    text = command.id + text?.let { " $it" }.orEmpty(),
                    entities = listOf(MessageEntity(MessageEntityType.BotCommand, 0, command.id.length)),
                ),
            ),
            HttpRequest(headers = emptyMap(), parameters = emptyMap()),
        )

        successfulResponse(response)
        assertions(TestCommandContext(actionSpy, eventSpy, messageSpy, setFlags))
    }

    private fun successfulResponse(response: RestResponse<Status>) {
        assertEquals(200, response.status, "Response should return a 200 status")
        assertEquals(0, response.data.code, "Response should return a zero-status code")
    }
}
