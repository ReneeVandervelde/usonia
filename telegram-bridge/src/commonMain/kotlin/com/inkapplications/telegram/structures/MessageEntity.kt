package com.inkapplications.telegram.structures

import kotlinx.serialization.Serializable

/**
 * This object represents one special entity in a text message.
 *
 * For example, hashtags, usernames, URLs, etc.
 */
@Serializable
data class MessageEntity(
    /**
     * Type of the entity.
     */
    val type: MessageEntityType,

    /**
     * Offset in UTF-16 code units to the start of the entity
     */
    val offset: Int,

    /**
     * Length of the entity in UTF-16 code units
     */
    val length: Int,

    /**
     * Optional. For “text_link” only, URL that will be opened after user taps on the text
     */
    val url: String? = null,

    /**
     * Optional. For “text_mention” only, the mentioned user
     */
    val user: User? = null,

    /**
     * Optional. For “pre” only, the programming language of the entity text
     */
    val language: String? = null,
)
