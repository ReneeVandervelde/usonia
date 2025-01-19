package com.inkapplications.telegram.structures

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class StickerParameters(
    /**
     * Unique identifier for the target chat or username of the target channel
     */
    @SerialName("chat_id")
    val chatId: ChatReference,

    /**
     * Sticker to send.
     */
    val sticker: InputFile,

    /**
     * Sends the message silently. Users will receive a notification with
     * no sound.
     */
    @SerialName("disable_notification")
    val disableNotification: Boolean? = null,

    /**
     * Protects the contents of the sent message from forwarding and saving
     */
    @SerialName("protect_content")
    val protectContent: Boolean? = null,

    /**
     * If the message is a reply, ID of the original message
     */
    @SerialName("reply_to_message_id")
    val replyToMessageId: Long? = null,

    /**
     * Pass True, if the message should be sent even if the specified
     * replied-to message is not found
     */
    @SerialName("allow_sending_without_reply")
    val allowSendingWithoutReply: Boolean? = null,

    // TODO: ReplyMarkup
)
