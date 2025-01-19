package com.inkapplications.telegram.structures

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Parameters when sending a message to the API
 */
@Serializable
data class MessageParameters(
    /**
     * Unique identifier for the target chat or username of the target channel
     * (in the format @channelusername)
     */
    @SerialName("chat_id")
    val chatId: ChatReference,
    /**
     * Text of the message to be sent, 1-4096 characters after entities parsing.
     */
    val text: String,

    /**
     * Mode for parsing entities in the message text.
     */
    @SerialName("parse_mode")
    val parseMode: ParseMode? = null,

    /**
     *  List of special entities that appear in message text, which can be
     *  specified instead of parse_mode
     */
    val entities: List<MessageEntity>? = null,

    /**
     * Disables link previews for links in this message
     */
    @SerialName("disable_web_page_preview")
    val disableWebPagePreview: Boolean? = null,

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
     * Pass True, if the message should be sent even if the specified replied-to
     * message is not found
     */
    @SerialName("allow_sending_without_reply")
    val allowSendingWithoutReply: Boolean? = null,

    // TODO: ReplyMarkup
)
