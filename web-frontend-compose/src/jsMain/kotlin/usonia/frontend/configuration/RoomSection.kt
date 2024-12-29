package usonia.frontend.configuration

import androidx.compose.runtime.Composable
import com.inkapplications.coroutines.ongoing.map
import org.jetbrains.compose.web.dom.*
import usonia.client.FrontendClient
import usonia.foundation.ParameterBag
import usonia.foundation.Room
import usonia.foundation.Site
import usonia.foundation.findRoom
import usonia.frontend.extensions.collectAsState
import usonia.frontend.navigation.NavigationSection
import usonia.frontend.navigation.Routing
import usonia.frontend.widgets.KeyValue
import usonia.frontend.widgets.LoadingIndicator

class RoomSection(
    private val client: FrontendClient,
): NavigationSection {
    override val route: Routing = Routing.TopLevel(
        route = "/rooms",
        title = "Rooms",
    )

    @Composable
    override fun renderContent(args: ParameterBag) {
        val state = client.site
            .map { ViewState.Loaded(it) as ViewState }
            .collectAsState(ViewState.Initial)
            .value

        when (state) {
            is ViewState.Initial -> LoadingIndicator()
            is ViewState.Loaded -> {
                H1 { Text("${state.site.name} Rooms")}
                state.site.rooms
                    .sortedBy { it.name }
                    .forEach { room ->
                        RoomDetails(
                            room,
                            site = state.site,
                        )
                    }
            }
        }
    }

    private sealed interface ViewState {
        object Initial: ViewState
        data class Loaded(val site: Site): ViewState
    }
}

@Composable
fun RoomDetails(
    room: Room,
    site: Site,
) {
    H2 {
        Text(room.name)
    }
    KeyValue("ID", room.id.value)
    Br()
    KeyValue("Type", room.type.name)
    Br()
    room.adjacentRooms
        .map { site.findRoom(it)?.name }
        .filterNotNull()
        .takeIf { it.isNotEmpty() }
        ?.joinToString()
        ?.run {
            KeyValue("Adjacent Rooms", this)
        }
    H3 {
        Text("Devices")
    }
    Ul {
        room.devices.forEach { device ->
            Li {
                A(
                    href = "/devices/${device.id}",
                ) {
                    Text(device.name)
                }
            }
        }
    }
}
