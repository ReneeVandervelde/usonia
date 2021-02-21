package usonia.frontend.metrics

import kimchi.logger.KimchiLogger
import kotlinx.browser.document
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import mustache.Mustache
import org.w3c.dom.HTMLCanvasElement
import usonia.chart.*
import usonia.client.FrontendClient
import usonia.foundation.Event
import usonia.foundation.Fixture
import usonia.foundation.Room
import usonia.frontend.ViewController
import usonia.js.accentColor
import usonia.js.awaitElement
import kotlin.math.abs
import kotlin.time.ExperimentalTime
import kotlin.time.days

@OptIn(ExperimentalTime::class)
class TemperatureMetricsController(
    private val client: FrontendClient,
    private val logger: KimchiLogger,
): ViewController {
    private val container by lazy { document.getElementById("metrics-temperatures") }
    private val template by lazy { document.getElementById("metrics-template-temperature")?.innerHTML }

    override suspend fun bind() {
        client.site.collectLatest { site ->
            container?.innerHTML = ""
            coroutineScope {
                site.rooms
                    .filter { it.devices.any { Event.Temperature::class in it.capabilities.events } }
                    .forEach { room ->
                        launch { bindGraph(room) }
                    }
            }
        }
    }

    private suspend fun bindGraph(room: Room) {
        val graphHtml = template?.let { Mustache.render(it, room) } ?: return
        val chartElement = coroutineScope {
            val deferred = async { awaitElement("metrics-temperature-canvas-${room.id.value}") as HTMLCanvasElement }
            container?.innerHTML += graphHtml
            deferred.await()
        }

        val data = DataSet(
            label = "Temperature",
            borderColor = chartElement.accentColor,
        )
        val chart = Chart(chartElement, Config(
            data = Data(
                datasets = arrayOf(data),
            ),
            options = Options(
                scales = ScaleOptions(
                    xAxes = arrayOf(
                        AxisOptions(
                            gridLines = GridLineOptions(display = false),
                            ticks = TickOptions(display = false)
                        ),
                    ),
                    yAxes = arrayOf(
                        AxisOptions(
                            gridLines = GridLineOptions(display = false),
                            ticks = TickOptions(precision = 0),
                        ),
                    ),
                ),
                legend = LegendOptions(
                    display = false,
                ),
            )
        ))

        val devices = room.devices
            .filter { Event.Temperature::class in it.capabilities.events }
            .filter { it.fixture !in setOf(Fixture.Refrigerator, Fixture.Freezer) }
            .map { it.id }

        client.temperatureHistory(devices)
            .map { it.filter { abs(it.key) < 7.days.inHours.toInt() } }
            .collectLatest { metrics ->
                logger.debug("Binding ${metrics.size} temperature points to ${room.name}")
                chart.data.labels = metrics.keys
                    .sorted()
                    .map { it.toString() }
                    .toTypedArray()
                data.data = metrics.keys
                    .sorted()
                    .map { metrics.getValue(it).toInt() }
                    .toTypedArray()
                chart.update()
            }
    }
}

