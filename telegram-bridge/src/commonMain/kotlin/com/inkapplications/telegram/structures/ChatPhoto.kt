package com.inkapplications.telegram.structures

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * This object represents a chat photo.
 */
@Serializable
data class ChatPhoto(
    /**
     * File identifier of small (160x160) chat photo.
     *
     * This file_id can be used only for photo download and only for as long as the photo is not changed.
     */
    @SerialName("small_file_id")
    val smallFileId: String,

    /**
     * Unique file identifier of small (160x160) chat photo, which is
     * supposed to be the same over time and for different bots.
     *
     * Can't be used to download or reuse the file.
     */
    @SerialName("small_file_unique_id")
    val smallFileUuid: String,

    /**
     * File identifier of big (640x640) chat photo.
     *
     * This file_id can be used only for photo download and only for
     * as long as the photo is not changed.
     */
    @SerialName("big_file_id")
    val bigFileId: String,

    /**
     * Unique file identifier of big (640x640) chat photo, which is
     * supposed to be the same over time and for different bots.
     *
     * Can't be used to download or reuse the file.
     */
    @SerialName("big_file_unique_id")
    val bigFileUuid: String,
)
