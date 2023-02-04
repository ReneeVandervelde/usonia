package usonia.frontend.configuration

import androidx.compose.runtime.Composable
import kotlinx.datetime.Clock
import org.jetbrains.compose.web.dom.*
import usonia.client.FrontendClient
import usonia.foundation.Device
import usonia.foundation.Event
import usonia.frontend.extensions.collectAsState
import usonia.frontend.navigation.NavigationSection
import usonia.kotlin.map

class ConfigurationSection(
    private val client: FrontendClient,
    private val clock: Clock = Clock.System,
): NavigationSection {
    override val title: String = "Configuration"

    @Composable
    override fun renderContent() {
        val roomsState = client.site
            .map { it.rooms }
            .map { it.toList() }
            .map { it.sortedBy { it.name } }
            .collectAsState(emptyList())

        val events = client.events

        roomsState.value.forEach { room ->
            Article {
                H2 { Text(room.name) }
                room.devices.forEach { device ->
                    DeviceDetails(device)
                }
            }
        }
    }

    @Composable
    private fun DeviceDetails(device: Device) {
        val latestEvent = client.getLatestEvent(device.id)
            .map { it as Event? }
            .collectAsState(null)
            .value

        H3 { Text(device.name) }
        if (latestEvent == null) {
            Text("Not seen recently")
        } else {
            val duration = clock.now().minus(latestEvent.timestamp)
            Text("Latest Event: ${latestEvent::class.simpleName} event $duration ago")
        }
    }

    @Composable
    private fun EventText(latest: Event) {
        val duration = clock.now().minus(latest.timestamp)
        Text("Latest Event: ${latest::class.simpleName} event $duration ago")
    }
}
