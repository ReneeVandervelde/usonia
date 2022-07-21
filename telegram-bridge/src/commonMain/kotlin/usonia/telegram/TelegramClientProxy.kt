package usonia.telegram

import com.inkapplications.telegram.client.TelegramBotClient
import com.inkapplications.telegram.structures.*
import java.util.concurrent.atomic.AtomicReference

internal class TelegramClientProxy: TelegramBotClient {
    private val delegateReference = AtomicReference<TelegramBotClient?>(null)
    var delegate: TelegramBotClient
        get() = delegateReference.get()!!
        set(value) { delegateReference.set(value) }

    override suspend fun getWebhookInfo(): WebhookInfo = delegate.getWebhookInfo()
    override suspend fun sendMessage(parameters: MessageParameters): Message = delegate.sendMessage(parameters)
    override suspend fun setWebhook(parameters: WebhookParameters) = delegate.setWebhook(parameters)
    override suspend fun sendSticker(parameters: StickerParameters): Message = delegate.sendSticker(parameters)
}

