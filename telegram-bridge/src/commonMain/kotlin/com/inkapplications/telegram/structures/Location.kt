package com.inkapplications.telegram.structures

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration

/**
 * This object represents a point on the map.
 */
@Serializable
data class Location(
    /**
     * Longitude as defined by sender
     */
    val longitude: Float,

    /**
     * Latitude as defined by sender
     */
    val latitude: Float,

    /**
     * Optional. The radius of uncertainty for the location,
     * measured in meters; 0-1500
     */
    @SerialName("horizontal_accuracy")
    val horizontalAccuracy: Float? = null,

    /**
     * Optional. Time relative to the message sending date, during which
     * the location can be updated.
     *
     * For active live locations only.
     */
    @SerialName("live_period")
    @Serializable(with = IntSecondsDurationSerializer::class)
    val livePeriod: Duration? = null,

    /**
     * Optional. The direction in which user is moving, in degrees; 1-360.
     *
     * For active live locations only.
     */
    val heading: Int? = null,

    /**
     * Optional. The maximum distance for proximity alerts about approaching
     * another chat member, in meters.
     *
     * For sent live locations only.
     */
    @SerialName("proximity_alert_radius")
    val proximityAlertRadius: Int? = null,
)
