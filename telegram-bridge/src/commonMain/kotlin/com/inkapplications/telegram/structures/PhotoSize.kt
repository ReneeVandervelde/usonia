package com.inkapplications.telegram.structures

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * This object represents one size of a photo or a file / sticker thumbnail.
 */
@Serializable
data class PhotoSize(
    /**
     * Identifier for this file, which can be used to download or reuse the file
     */
    @SerialName("file_id")
    val fileId: String,

    /**
     * Unique identifier for this file, which is supposed to be the same over
     * time and for different bots.
     *
     * Can't be used to download or reuse the file.
     */
    @SerialName("file_unique_id")
    val fileUuid: String,

    /**
     * Photo width
     */
    val width: Int,

    /**
     * Photo height
     */
    val height: Int,

    /**
     * Optional. File size in bytes
     */
    @SerialName("file_size")
    val fileSize: Int? = null,
)
