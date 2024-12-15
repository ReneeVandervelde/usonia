package usonia.frontend.metrics

import androidx.compose.runtime.Composable
import chart.LineChart
import chart.LineChartConfig
import inkapplications.spondee.measure.us.toFahrenheit
import kimchi.logger.KimchiLogger
import org.jetbrains.compose.web.dom.*
import usonia.client.FrontendClient
import usonia.core.state.roomTemperatureHistory
import usonia.core.state.rooms
import usonia.foundation.ParameterBag
import usonia.frontend.extensions.collectAsState
import usonia.frontend.navigation.NavigationSection
import usonia.frontend.navigation.Routing
import usonia.kotlin.map
import kotlin.time.DurationUnit

class MetricsSection(
    private val client: FrontendClient,
    private val logger: KimchiLogger,
): NavigationSection {
    override val route: Routing = Routing.TopLevel(
        route = "/metrics",
        title = "Metrics",
    )

    private val eventsData = client.eventsByDay
        .map {
            it.mapKeys { it.toString() }.toList()
        }
    private val rooms = client.rooms
        .map { it.sortedBy { it.name } }

    @Composable
    override fun renderContent(args: ParameterBag) {
        val eventState = eventsData.collectAsState(listOf())
        val rooms = rooms.collectAsState(listOf())

        H1 { Text("Metrics") }
        H2 {
            Text("Events")
        }
        LineChart(
            dataSet = eventState.value
        )

        H2 {
            Text("Temperature")
        }
        P {
            Text("Last 14 days of data")
        }
        rooms.value.forEach { room ->
            H3 { Text(room.name) }
            val temperatureData = client.roomTemperatureHistory(room)
                .map { it.map { it.timeAgo.toInt(DurationUnit.HOURS).toString() to it.temperature.toFahrenheit().value } }
                .collectAsState(emptyList())

            LineChart(
                dataSet = temperatureData.value,
                initConfig = LineChartConfig(
                    xTicksVisible = false,
                )
            )
        }
    }
}
