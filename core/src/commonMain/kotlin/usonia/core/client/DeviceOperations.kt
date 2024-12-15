package usonia.core.client

import kotlinx.coroutines.flow.onStart
import usonia.foundation.Device
import usonia.foundation.Event
import usonia.foundation.Identifier
import usonia.foundation.getDevice
import usonia.kotlin.*

/**
 * Listen to a device's latest events
 */
fun UsoniaClient.deviceEvents(id: Identifier, limit: Int? = null): OngoingFlow<DeviceEvents> {
    return site.map { it.getDevice(id) }
        .combineWith(deviceEventHistory(id, limit)) { device, events ->
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

inline fun <reified T: Event> UsoniaClient.latestDeviceEventOfType(device: Device): OngoingFlow<DeviceProperty<T?>> {
    return events.filterIsInstance<T>()
        .filter { it.source == device.id }
        .map { it as T? }
        .unsafeModify { onStart { emit(getState(device.id, T::class)) } }
        .map { DeviceProperty(device, it) }
}


data class DeviceProperty<T>(
    val device: Device,
    val event: T,
)
