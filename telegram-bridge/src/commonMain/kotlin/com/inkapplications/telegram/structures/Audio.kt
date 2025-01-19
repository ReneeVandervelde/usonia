package com.inkapplications.telegram.structures

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration

/**
 * This object represents an audio file to be treated as music by the
 * Telegram clients.
 */
@Serializable
data class Audio(
    /**
     * Identifier for this file, which can be used to download or reuse the file
     */
    @SerialName("file_id")
    val fileId: String,

    /**
     * Unique identifier for this file, which is supposed to be the same
     * over time and for different bots.
     *
     * Can't be used to download or reuse the file.
     */
    @SerialName("file_unique_id")
    val fileUid: String,

    /**
     * Duration of the audio in seconds as defined by sender
     */
    @Serializable(with = IntSecondsDurationSerializer::class)
    val duration: Duration,

    /**
     * Optional. Performer of the audio as defined by sender or by audio tags
     */
    val performer: String? = null,

    /**
     * Optional. Title of the audio as defined by sender or by audio tags
     */
    val title: String? = null,

    /**
     * Optional. Original filename as defined by sender
     */
    @SerialName("file_name")
    val fileName: String? = null,

    /**
     * Optional. MIME type of the file as defined by sender
     */
    @SerialName("mime_type")
    val mimeType: MimeType? = null,

    /**
     * Optional. File size in bytes
     */
    @SerialName("file_size")
    val fileSize: Long? = null,

    /**
     * Optional. Thumbnail of the album cover to which the music file belongs
     */
    @SerialName("thumb")
    val thumbnail: PhotoSize? = null,
)
