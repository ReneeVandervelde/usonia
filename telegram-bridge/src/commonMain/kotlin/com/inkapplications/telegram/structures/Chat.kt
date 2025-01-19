package com.inkapplications.telegram.structures

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration

/**
 * A chat conversation.
 */
@Serializable
data class Chat(
    /**
     * Unique identifier for this chat.
     */
    val id: ChatReference.Id,

    /**
     * Type of chat
     */
    val type: ChatType,

    /**
     * Optional. Title, for supergroups, channels and group chats
     */
    val title: String? = null,

    /**
     * Optional. Username, for private chats, supergroups and channels if available
     */
    val username: String? = null,

    /**
     * Optional. First name of the other party in a private chat
     */
    @SerialName("first_name")
    val firstName: String? = null,

    /**
     * Optional. Last name of the other party in a private chat
     */
    @SerialName("last_name")
    val lastName: String? = null,

    /**
     * Optional. Chat photo.
     *
     * Returned only in getChat.
     */
    val photo: ChatPhoto? = null,

    /**
     * Optional. Bio of the other party in a private chat.
     *
     * Returned only in getChat.
     */
    val bio: String? = null,

    /**
     * Optional. True, if privacy settings of the other party in the private
     * chat allows to use `tg://user?id=<user_id>` links only in chats with
     * the user.
     *
     * Returned only in getChat.
     */
    @SerialName("has_private_forwards")
    val hasPrivateForwards: Boolean? = null,

    /**
     * Optional. True, if users need to join the supergroup before they
     * can send messages.
     *
     * Returned only in getChat.
     */
    @SerialName("join_to_send_messages")
    val joinToSendMessages: Boolean? = null,

    /**
     * Optional. True, if all users directly joining the supergroup need
     * to be approved by supergroup administrators.
     *
     * Returned only in getChat.
     */
    @SerialName("join_by_request")
    val joinByRequest: Boolean? = null,

    /**
     * Optional. Description, for groups, supergroups and channel chats.
     *
     * Returned only in getChat.
     */
    val description: String? = null,

    /**
     * Optional. Primary invite link, for groups, supergroups and channel chats.
     *
     * Returned only in getChat.
     */
    @SerialName("invite_link")
    val inviteLink: String? = null,

    /**
     * Optional. The most recent pinned message (by sending date).
     *
     * Returned only in getChat.
     */
    @SerialName("pinned_message")
    val pinnedMessage: Message? = null,

    /**
     * Optional. Default chat member permissions, for groups and supergroups.
     *
     * Returned only in getChat.
     */
    val permissions: ChatPermissions? = null,

    /**
     * Optional. For supergroups, the minimum allowed delay between consecutive
     * messages sent by each unpriviledged user
     *
     * Returned only in getChat.
     */
    @SerialName("slow_mode_delay")
    @Serializable(with = IntSecondsDurationSerializer::class)
    val slowModeDelay: Duration? = null,

    /**
     * Optional. The time after which all messages sent to the chat will
     * be automatically deleted; in seconds.
     *
     * Returned only in getChat.
     */
    @SerialName("message_auto_delete_time")
    @Serializable(with = IntSecondsDurationSerializer::class)
    val messageAutoDeleteTime: Duration? = null,

    /**
     * Optional. True, if messages from the chat can't be forwarded to other chats. Returned only in getChat.
     */
    @SerialName("has_protected_content")
    val hasProtectedContent: Boolean? = null,

    /**
     * Optional. For supergroups, name of group sticker set.
     *
     * Returned only in getChat.
     */
    @SerialName("sticker_set_name")
    val stickerSetName: String? = null,

    /**
     * Optional. True, if the bot can change the group sticker set.
     *
     * Returned only in getChat.
     */
    @SerialName("can_set_sticker_set")
    val canSetStickerSet: Boolean? = null,

    /**
     * Optional. Unique identifier for the linked chat
     *
     * i.e. the discussion group identifier for a channel and vice versa;
     * for supergroups and channel chats.
     *
     * Returned only in getChat.
     */
    @SerialName("linked_chat_id")
    val linkedChatId: Long? = null,

    /**
     * Optional. For supergroups, the location to which the supergroup
     * is connected.
     *
     * Returned only in getChat.
     */
    val location: ChatLocation? = null,
)
