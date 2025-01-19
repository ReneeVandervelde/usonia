package com.inkapplications.telegram.structures

import kotlinx.serialization.Serializable

/**
 * Represents a location to which a chat is connected.
 */
@Serializable
data class ChatLocation(
    /**
     * The location to which the supergroup is connected.
     *
     * Can't be a live location.
     */
    val location: Location,

    /**
     * Location address; 1-64 characters, as defined by the chat owner
     */
    val address: String,
)
