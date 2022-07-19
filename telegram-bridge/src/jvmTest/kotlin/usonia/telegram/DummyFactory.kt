package usonia.telegram

import com.inkapplications.telegram.client.TelegramBotClient

class DummyFactory<T: TelegramBotClient>(
    val client: T
): ClientFactory {
    override fun create(key: String, token: String): TelegramBotClient = client
}
