package usonia.core.client

import usonia.foundation.Device
import usonia.foundation.Event
import usonia.foundation.Identifier
import usonia.foundation.getDevice
import usonia.kotlin.OngoingFlow
import usonia.kotlin.combine
import usonia.kotlin.map

/**
 * Listen to a device's latest events
 */
fun UsoniaClient.deviceEvents(id: Identifier, limit: Int? = null): OngoingFlow<DeviceEvents> {
    return site.map { it.getDevice(id) }
        .combine(deviceEventHistory(id, limit)) { device, events ->
            DeviceEvents(
                device = device,
                events = events,
            )
        }
}

/**
 * Data structure of a device along with its event set.
 */
data class DeviceEvents(
    val device: Device,
    val events: List<Event>,
)
