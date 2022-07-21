package usonia.telegram

import com.inkapplications.telegram.client.TelegramBotClient
import com.inkapplications.telegram.structures.*

object ClientStub: TelegramBotClient {
    override suspend fun getWebhookInfo(): WebhookInfo = TODO()
    override suspend fun sendMessage(parameters: MessageParameters): Message = TODO()
    override suspend fun sendSticker(parameters: StickerParameters): Message = TODO()
    override suspend fun setWebhook(parameters: WebhookParameters) = TODO()
}
