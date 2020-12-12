package usonia.frontend.config

import usonia.foundation.Site
import usonia.kotlin.mapArray

data class ConfigViewModel(
    val name: String,
    val rooms: Array<RoomViewModel>,
) {
    constructor(site: Site): this(
        name = site.name,
        rooms = site.rooms.mapArray { room ->
            RoomViewModel(
                name = room.name,
                devices = room.devices.mapArray { device ->
                    DeviceViewModel(
                        name = device.name
                    )
                }
            )
        }
    )
}
