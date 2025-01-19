package com.inkapplications.telegram.structures

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * This object represents an incoming inline query.
 *
 * When the user sends an empty query, your bot could return some default
 * or trending results.
 */
@Serializable
data class InlineQuery(
    /**
     * Unique identifier for this query
     */
    val id: String,

    /**
     * Sender
     */
    val from: User,

    /**
     * Text of the query (up to 256 characters)
     */
    val query: String,

    /**
     * Offset of the results to be returned, can be controlled by the bot
     */
    val offset: String,

    /**
     * Optional. Type of the chat from which the inline query was sent.
     *
     * The chat type should be always known for requests sent from official
     * clients and most third-party clients, unless the request was sent
     * from a secret chat
     */
    @SerialName("chat_type")
    val chatType: ChatType? = null,

    /**
     * Optional. Sender location, only for bots that request user location
     */
    val location: Location? = null,
)
