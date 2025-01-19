package com.inkapplications.telegram.structures

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A message sent in the application.
 */
@Serializable
data class Message(
    /**
     * Unique message identifier inside this chat
     */
    @SerialName("message_id")
    val id: ChatReference.Id,

    /**
     * Date the message was sent
     */
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    val date: Instant,

    /**
     * Conversation the message belongs to
     */
    val chat: Chat,

    /**
     * Optional. Sender of the message; empty for messages sent to channels.
     *
     * For backward compatibility, the field contains a fake sender user
     * in non-channel chats, if the message was sent on behalf of a chat.
     */
    val from: User? = null,

    /**
     * Optional. Sender of the message, sent on behalf of a chat.
     *
     * For example, the channel itself for channel posts, the supergroup itself
     * for messages from anonymous group administrators, the linked channel
     * for messages automatically forwarded to the discussion group.
     *
     * For backward compatibility, the field from contains a fake sender user
     * in non-channel chats, if the message was sent on behalf of a chat.
     */
    @SerialName("sender_chat")
    val senderChat: Chat? = null,

    /**
     * Optional. For forwarded messages, sender of the original message
     */
    @SerialName("forward_from")
    val forwardFrom: User? = null,

    /**
     * Optional. For messages forwarded from channels or from anonymous
     * administrators, information about the original sender chat
     */
    @SerialName("forward_from_chat")
    val forwardFromChat: Chat? = null,

    /**
     * Optional. For messages forwarded from channels, identifier of the
     * original message in the channel
     */
    @SerialName("forward_from_message_id")
    val forwardFromMessageId: Long? = null,

    /**
     * Optional. For forwarded messages that were originally sent in channels
     * or by an anonymous chat administrator, signature of the message sender
     * if present
     */
    @SerialName("forward_signature")
    val forwardSignature: String? = null,

    /**
     * Optional. Sender's name for messages forwarded from users who disallow
     * adding a link to their account in forwarded messages
     */
    @SerialName("forward_sender_name")
    val forwardSenderName: String? = null,

    /**
     * Optional. For forwarded messages, date the original message was sent
     */
    @SerialName("forward_date")
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    val forwardDate: Instant? = null,

    /**
     * Optional. Whether the message is a channel post that was automatically
     * forwarded to the connected discussion group
     */
    @SerialName("is_automatic_forward")
    val isAutomaticForward: Boolean? = null,

    /**
     * Optional. For replies, the original message. Note that the Message
     * object in this field will not contain further reply_to_message fields
     * even if it itself is a reply.
     */
    @SerialName("reply_to_message")
    val replyToMessage: Message? = null,

    /**
     * Optional. Bot through which the message was sent
     */
    @SerialName("via_bot")
    val viaBot: User? = null,

    /**
     * Optional. Date the message was last edited
     */
    @SerialName("edit_date")
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    val editDate: Instant? = null,

    /**
     * Optional. True, if the message can't be forwarded
     */
    @SerialName("has_protected_content")
    val hasProtectedContent: Boolean? = null,

    /**
     * Optional. The unique identifier of a media message group this message
     * belongs to
     */
    @SerialName("media_group_id")
    val mediaGroupId: String? = null,

    /**
     * Optional. Signature of the post author for messages in channels, or the
     * custom title of an anonymous group administrator
     */
    @SerialName("author_signature")
    val authorSignature: String? = null,

    /**
     * Optional. For text messages, the actual UTF-8 text of the message
     */
    val text: String? = null,

    /**
     * Optional. For text messages, special entities like usernames, URLs,
     * bot commands, etc. that appear in the text
     */
    val entities: List<MessageEntity>? = null,

    /**
     * Optional. Message is an animation, information about the animation.
     *
     * For backward compatibility, when this field is set, the document field
     * will also be set
     */
    val animation: Animation? = null,

    /**
     * Optional. Message is an audio file, information about the file
     */
    val audio: Audio? = null,

    // TODO: There are plenty more fields here to be added.
)
