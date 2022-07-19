package usonia.telegram

import com.inkapplications.telegram.client.TelegramBotClient

/**
 * Create instances of the telegram client when the token changes.
 */
internal interface ClientFactory {
    fun create(key: String, token: String): TelegramBotClient
}
