package usonia.frontend.metrics

import kimchi.logger.KimchiLogger
import kotlinx.browser.document
import org.w3c.dom.HTMLCanvasElement
import usonia.chart.*
import usonia.client.FrontendClient
import usonia.frontend.Controller
import usonia.js.accentColor
import usonia.kotlin.collectLatest

class EventMetricsController(
    private val client: FrontendClient,
    private val logger: KimchiLogger,
): Controller {
    private val eventsByDayChart by lazy { document.getElementById("metrics-event-graph") as HTMLCanvasElement? }

    override suspend fun onReady() {
        val canvas = eventsByDayChart ?: run {
            logger.debug("No Events metric to bind to.")
            return
        }
        val eventData = DataSet(
            label = "Events",
            borderColor = canvas.accentColor,
        )
        val chart = Chart(canvas, Config(
            data = Data(
                datasets = arrayOf(eventData),
            ),
            options = Options(
                scales = ScaleOptions(
                    xAxes = arrayOf(
                        AxisOptions(gridLines = GridLineOptions(display = false)),
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
        client.eventsByDay.collectLatest { metrics ->
            logger.debug("Binding ${metrics.size} event points")
            chart.data.labels = metrics.keys
                .sorted()
                .map { it.toString() }
                .toTypedArray()
            eventData.data = metrics.keys
                .sorted()
                .map { metrics.getValue(it) }
                .toTypedArray()
            chart.update()
        }
    }
}
