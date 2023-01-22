package usonia.frontend.metrics

import androidx.compose.runtime.Composable
import chart.*
import kimchi.logger.KimchiLogger
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import usonia.client.FrontendClient
import usonia.foundation.Event
import usonia.foundation.Fixture
import usonia.frontend.extensions.collectAsState
import usonia.frontend.navigation.NavigationSection
import usonia.kotlin.map
import kotlin.math.abs
import kotlin.time.Duration.Companion.days
import kotlin.time.DurationUnit

class MetricsSection(
    private val client: FrontendClient,
    private val logger: KimchiLogger,
): NavigationSection {
    override val title: String = "Metrics"

    private val eventsData = client.eventsByDay
        .map {
            it.mapKeys { it.toString() }.toList()
        }
    private val rooms = client.site
        .map { it.rooms }
        .map { it.toList() }
        .map { it.sortedBy { it.name } }

    @Composable
    override fun renderContent() {
        val eventState = eventsData.collectAsState(listOf())
        val rooms = rooms.collectAsState(listOf())

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
            val temperatureData = room.devices
                .filter { Event.Temperature::class in it.capabilities.events }
                .filter { it.fixture !in setOf(Fixture.Refrigerator, Fixture.Freezer) }
                .map { it.id }
                .let { client.temperatureHistory(it) }
                .map { it.filter { abs(it.key) < 14.days.toInt(DurationUnit.HOURS) } }
                .map { it.toList() }
                .map { it.map { it.first.toString() to it.second } }
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
