package usonia.telegram

import io.ktor.client.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

private const val HOST = "api.telegram.org"

internal interface TelegramApi {
    suspend fun sendMessage(
        bot: String,
        token: String,
        chatId: String,
        message: String,
    )
}

@OptIn(ExperimentalTime::class)
internal object TelegramClient: TelegramApi {
    private val client = HttpClient {
        install(HttpTimeout) {
            requestTimeoutMillis = 30.seconds.toLongMilliseconds()
        }
    }

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
