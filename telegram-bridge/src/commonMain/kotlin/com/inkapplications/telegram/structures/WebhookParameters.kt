package com.inkapplications.telegram.structures

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

/**
 * Options sent to The API when setting up a webhook.
 */
@Serializable
data class WebhookParameters(
    /**
     * HTTPS URL to send updates to. Use an empty string to remove
     * webhook integration
     */
    val url: String,

    /**
     * Upload your public key certificate so that the root certificate in
     * use can be checked. See our [self-signed guide] for details.
     *
     * [self-signed guide]:https://core.telegram.org/bots/self-signed
     */
    val certificate: InputFile? = null,

    /**
     * The fixed IP address which will be used to send webhook requests
     * instead of the IP address resolved through DNS
     */
    @SerialName("ip_address")
    val ipAddress: String? = null,

    /**
     * The maximum allowed number of simultaneous HTTPS connections to the
     * webhook for update delivery, 1-100.
     *
     * Defaults to 40. Use lower values to limit the load on your bot's
     * server, and higher values to increase your bot's throughput.
     */
    @SerialName("max_connections")
    val maxConnections: Int? = null,

    /**
     * A List of the update types you want your bot to receive.
     */
    @SerialName("allowed_updates")
    @Serializable(with = Update.TypeListSerializer::class)
    val allowedUpdates: List<KClass<out Update>>? = null,

    /**
     * Pass True to drop all pending updates
     */
    @SerialName("drop_pending_updates")
    val dropPendingUpdates: Boolean? = null,

    /**
     * A secret token to be sent in a header “X-Telegram-Bot-Api-Secret-Token”
     * in every webhook request, 1-256 characters.
     *
     * Only characters `A-Z`, `a-z`, `0-9`, `_` and `-` are allowed.
     *
     * The header is useful to ensure that the request comes from a webhook
     * set by you.
     */
    @SerialName("secret_token")
    val secretToken: String? = null,
)
