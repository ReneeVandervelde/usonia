package usonia.telegram

import com.inkapplications.telegram.client.TelegramBotClient
import com.inkapplications.telegram.structures.*
import kotlinx.datetime.Clock

class MessageSpy: TelegramBotClient by ClientStub {
    val messages = mutableListOf<MessageParameters>()
    override suspend fun sendMessage(parameters: MessageParameters): Message {
        messages.add(parameters)
        return Message(
            id = ChatReference.Id(0L),
            date = Clock.System.now(),
            chat = Chat(ChatReference.Id(0L), ChatType.Private)
        )
    }
}
