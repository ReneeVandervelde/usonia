package usonia.frontend.configuration

import androidx.compose.runtime.Composable
import com.inkapplications.coroutines.ongoing.map
import inkapplications.spondee.measure.us.toFahrenheit
import inkapplications.spondee.scalar.toWholePercentage
import inkapplications.spondee.structure.format
import org.jetbrains.compose.web.dom.*
import usonia.client.FrontendClient
import usonia.core.client.deviceEvents
import usonia.foundation.*
import usonia.frontend.extensions.collectAsState
import usonia.frontend.navigation.NavigationSection
import usonia.frontend.navigation.Routing
import usonia.frontend.widgets.KeyValue
import usonia.frontend.widgets.LoadingIndicator

class DeviceSection(
    private val client: FrontendClient,
): NavigationSection {
    override val route: Routing = Routing.Dynamic(
        route = "/devices/{deviceId}"
    )

    @Composable
    override fun renderContent(args: ParameterBag) {
        val id = args["deviceId"]?.let(::Identifier)!!
        val deviceState = client.deviceEvents(id, limit = 20)
            .map { (device, events) ->
                ViewState.Loaded(device, events) as ViewState
            }
            .collectAsState(ViewState.Initial)

        when (val currentState = deviceState.value) {
            ViewState.Initial -> LoadingIndicator()
            is ViewState.Loaded -> DeviceDetails(currentState.device, currentState.events)
        }
    }

    @Composable
    private fun DeviceDetails(device: Device, events: List<Event>) {
        H2 { Text("Info") }
        KeyValue("Name", device.name)
        Br()
        KeyValue("ID", device.id.value)
        Br()
        device.parent?.run {
            KeyValue("Parent", toString())
            Br()
        }
        device.fixture?.run {
            KeyValue("Fixture", name)
            Br()
        }
        H3 { Text("Capabilities") }
        device.capabilities.archetypeId?.run {
            KeyValue("Archetype", this)
            Br()
        }
        device.capabilities.heartbeat?.run {
            KeyValue("Heartbeat", toString())
            Br()
        }
        KeyValue("Events", device.capabilities.events.map { it.simpleName }.joinToString().ifEmpty { "None" })
        Br()
        KeyValue("Actions", device.capabilities.actions.map { it.simpleName }.joinToString().ifEmpty { "None" })
        Br()

        H2 { Text("Recent Events") }
        val physicalEvents = client.eventCount(device.id, EventCategory.Physical).collectAsState(null).value
        if (physicalEvents != null && physicalEvents != 0L) {
            KeyValue("Physical Events", physicalEvents.toString())
        }
        Br()
        Table {
            Tr {
                Th {
                    Text("Name")
                }
                Th {
                    Text("State")
                }
                Th {
                    Text("Timestamp")
                }
            }
            events.forEach { event ->
                Tr {
                    Td {
                        Text(event::class.simpleName!!)
                    }
                    Td {
                        when (event) {
                            is Event.Battery -> Text(event.percentage.toWholePercentage().format())
                            is Event.Humidity -> Text(event.humidity.toWholePercentage().format())
                            is Event.Latch -> Text("${event.state}")
                            is Event.Lock -> Text("${event.state}")
                            is Event.Motion -> Text("${event.state}")
                            is Event.Movement -> Text("${event.state}")
                            is Event.Power -> Text("${event.power}")
                            is Event.Presence -> Text("${event.state}")
                            is Event.Pressure -> Text("${event.pressure}")
                            is Event.Switch -> Text("${event.state}")
                            is Event.Temperature -> Text(event.temperature.toFahrenheit().format())
                            is Event.Tilt -> Text("x: ${event.x} y: ${event.y} z: ${event.z}")
                            is Event.Water -> Text("${event.state}")
                            is Event.Valve -> Text("${event.state}")
                        }
                    }
                    Td {
                        Text(event.timestamp.toString())
                    }
                }
            }
        }
    }

    private sealed interface ViewState {
        object Initial: ViewState
        data class Loaded(
            val device: Device,
            val events: List<Event> = emptyList()
        ): ViewState
    }
}
