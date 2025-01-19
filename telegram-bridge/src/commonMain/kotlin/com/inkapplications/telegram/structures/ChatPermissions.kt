package com.inkapplications.telegram.structures

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Describes actions that a non-administrator user is allowed to take in a chat.
 */
@Serializable
data class ChatPermissions(
    /**
     * Optional. True, if the user is allowed to send text messages, contacts,
     * locations and venues
     */
    @SerialName("can_send_messages")
    val canSendMessages: Boolean? = null,

    /**
     * Optional. True, if the user is allowed to send audios, documents,
     * photos, videos, video notes and voice notes, implies [canSendMessages]
     */
    @SerialName("can_send_media_messages")
    val canSendMediaMessages: Boolean? = null,

    /**
     * Optional. True, if the user is allowed to send polls, implies [canSendMessages]
     * */
    @SerialName("can_send_polls")
    val canSend: Boolean? = null,
    /**
     * Optional. True, if the user is allowed to send animations, games,
     * stickers and use inline bots, implies [canSendMediaMessages]
     * */
    @SerialName("can_send_other_messages")
    val canSendOtherMessages: Boolean? = null,

    /**
     * Optional. True, if the user is allowed to add web page previews to
     * their messages, implies [canSendMediaMessages]
     * */
    @SerialName("can_add_web_page_previews")
    val can_add_web_page_previews: Boolean? = null,

    /**
     * Optional. True, if the user is allowed to change the chat title,
     * photo and other settings.
     *
     * Ignored in public supergroups
     * */
    @SerialName("can_change_info")
    val canChangeInfo: Boolean? = null,

    /**
     * Optional. True, if the user is allowed to invite new users to the chat
     * */
    @SerialName("can_invite_users")
    val canInviteUsers: Boolean? = null,

    /**
     * Optional. True, if the user is allowed to pin messages.
     *
     * Ignored in public supergroups
     * */
    @SerialName("can_pin_messages")
    val canPinMessages: Boolean? = null,
)
