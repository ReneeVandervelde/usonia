package com.inkapplications.telegram.structures

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * This object represents a Telegram user or bot.
 */
@Serializable
data class User(
    /**
     * Unique identifier for this user or bot.
     */
    val id: Long,

    /**
     * Whether this user is a bot
     */
    @SerialName("is_bot")
    val isBot: Boolean,

    /**
     * User's or bot's first name
     */
    @SerialName("first_name")
    val firstName: String,

    /**
     * Optional. User's or bot's last name
     */
    @SerialName("last_name")
    val lastName: String? = null,

    /**
     * Optional. User's or bot's username
     */
    val username: String? = null,

    /**
     * Optional. IETF language tag of the user's language
     */
    val language: Language? = null,

    /**
     * Optional. True, if this user is a Telegram Premium user
     */
    @SerialName("is_premium")
    val isPremium: Boolean? = null,

    /**
     * Optional. True, if this user added the bot to the attachment menu
     */
    @SerialName("added_to_attachment_menu")
    val addedToAttachmentMenu: Boolean? = null,

    /**
     * Optional. True, if the bot can be invited to groups.
     *
     * Returned only in getMe.
     */
    @SerialName("can_join_groups")
    val canJoinGroups: Boolean? = null,

    /**
     * Optional. True, if privacy mode is disabled for the bot.
     *
     * Returned only in getMe.
     */
    @SerialName("can_read_all_group_messages")
    val canReadAllGroupMessages: Boolean? = null,

    /**
     * Optional. True, if the bot supports inline queries.
     *
     * Returned only in getMe.
     */
    @SerialName("supports_inline_queries")
    val supportsInlineQueries: Boolean? = null,
)
