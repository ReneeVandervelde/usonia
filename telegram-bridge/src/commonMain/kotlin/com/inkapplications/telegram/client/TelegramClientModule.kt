package com.inkapplications.telegram.client

/**
 * Creates Client services from parameters.
 */
class TelegramClientModule {
    /**
     * Create a telegram bot client.
     *
     * @param token Your bot's secret token.
     */
    fun createClient(token: String): TelegramBotClient = KtorTelegramClient(token)
}
