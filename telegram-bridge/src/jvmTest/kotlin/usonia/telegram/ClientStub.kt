package usonia.telegram

import com.inkapplications.telegram.client.TelegramBotClient
import com.inkapplications.telegram.structures.Message
import com.inkapplications.telegram.structures.MessageParameters
import com.inkapplications.telegram.structures.WebhookInfo
import com.inkapplications.telegram.structures.WebhookParameters

object ClientStub: TelegramBotClient {
    override suspend fun getWebhookInfo(): WebhookInfo = TODO()
    override suspend fun sendMessage(parameters: MessageParameters): Message = TODO()
    override suspend fun setWebhook(parameters: WebhookParameters) = TODO()
}
