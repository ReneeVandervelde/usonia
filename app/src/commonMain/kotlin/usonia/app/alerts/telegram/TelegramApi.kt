package usonia.app.alerts.telegram

import io.ktor.client.*
import io.ktor.client.request.*

private const val HOST = "api.telegram.org"

interface TelegramApi {
    suspend fun sendMessage(
        bot: String,
        token: String,
        chatId: String,
        message: String,
    )
}

internal object TelegramClient: TelegramApi {
    private val client = HttpClient {}

    override suspend fun sendMessage(
        bot: String,
        token: String,
        chatId: String,
        message: String,
    ) {
        client.get<String>(
            host = HOST,
            path = "/$bot:$token/sendMessage",

        ) {
            parameter("chat_id", chatId)
            parameter("text", message)
        }
    }
}
