package com.inkapplications.telegram.structures

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration

/**
 * This object represents an animation file.
 *
 * ie. GIF or H.264/MPEG-4 AVC video without sound.
 */
@Serializable
data class Animation(
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
     * Video width as defined by sender
     */
    val width: Int,

    /**
     * Video height as defined by sender
     */
    val height: Int,

    /**
     * Duration of the video in seconds as defined by sender
     */
    @Serializable(with = IntSecondsDurationSerializer::class)
    val duration: Duration,

    /**
     * Optional. Animation thumbnail as defined by sender
     */
    @SerialName("thumb")
    val thumbnail: PhotoSize? = null,

    /**
     * Optional. Original animation filename as defined by sender
     */
    @SerialName("file_name")
    val fileName: String? = null,

    /**
     * Optional. MIME type of the file as defined by sender
     */
    @SerialName("mime_type")
    val mimeType: MimeType? = null,

    /**
     * Optional. File size in bytes.
     */
    @SerialName("file_size")
    val fileSize: Long? = null,
)
