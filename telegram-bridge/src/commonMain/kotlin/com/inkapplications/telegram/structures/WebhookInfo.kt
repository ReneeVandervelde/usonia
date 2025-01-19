package com.inkapplications.telegram.structures

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

/**
 * Describes the current status of a webhook.
 */
@Serializable
data class WebhookInfo(
    /**
     * Webhook URL, may be empty if webhook is not set up
     */
    val url: String,

    /**
     * True, if a custom certificate was provided for webhook certificate checks
     */
    @SerialName("has_custom_certificate")
    val hasCustomCertificate: Boolean,

    /**
     * Number of updates awaiting delivery
     */
    @SerialName("pending_update_count")
    val pendingUpdateCount: Int,

    /**
     * Optional. Currently used webhook IP address
     */
    @SerialName("ip_address")
    val ipAddress: String? = null,

    /**
     * Optional. The most recent error that happened when trying to deliver
     * an update via webhook
     */
    @SerialName("last_error_date")
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    val lastErrorDate: Instant? = null,

    /**
     * Optional. Error message in human-readable format for the most recent
     * error that happened when trying to deliver an update via webhook
     */
    @SerialName("last_error_message")
    val lastErrorMessage: String? = null,

    /**
     * Optional. The most recent error that happened when trying to synchronize
     * available updates with Telegram datacenters
     */
    @SerialName("last_synchronization_error_date")
    @Serializable(with = InstantEpochMillisecondsSerializer::class)
    val lastSynchronizationErrorDate: Instant? = null,

    /**
     * Optional. The maximum allowed number of simultaneous HTTPS connections
     * to the webhook for update delivery
     */
    @SerialName("max_connections")
    val maxConnections: Int? = null,

    /**
     * Optional. A list of update types the bot is subscribed to. Defaults to
     * all update types except chat_member
     */
    @SerialName("allowed_updates")
    @Serializable(with = Update.TypeListSerializer::class)
    val allowedUpdates: List<KClass<Update>>? = null,
)
