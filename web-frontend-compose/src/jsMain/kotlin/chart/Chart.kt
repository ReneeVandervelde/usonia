package chart

import androidx.compose.runtime.*
import org.jetbrains.compose.web.dom.Canvas
import org.w3c.dom.HTMLCanvasElement
import usonia.frontend.extensions.accentColor
import usonia.frontend.extensions.jso

@JsModule("chart.js/auto")
@JsNonModule
private external object ChartJs {
    class Chart(
        context: HTMLCanvasElement,
        config: Config,
    ) {
        var data: Data

        fun update()
    }

    interface Config {
        var data: Data
        var type: String
        var options: Options
    }

    interface Options {
        var scales: ScaleOptions
        var plugins: Plugins
        var elements: ElementOptions
    }

    interface Plugins {
        var legend: LegendOptions
    }

    interface LegendOptions {
        var display: Boolean
    }

    interface ScaleOptions {
        var x: AxisOptions
        var y: AxisOptions
    }

    interface AxisOptions {
        var beginAtZero: Boolean
        var grid: GridOptions
        var ticks: TickOptions
        var border: BorderOptions
    }

    interface BorderOptions {
        var display: Boolean
    }

    interface TickOptions {
        var display: Boolean
    }

    interface ElementOptions {
        var point: PointOptions
    }

    interface PointOptions {
        var pointStyle: dynamic
    }

    interface GridOptions {
        var display: Boolean
    }

    interface Data {
        var datasets: Array<DataSet>
        var labels: Array<String>
    }

    interface DataSet {
        var label: String
        var data: Array<Number>
        var borderColor: String
        var tension: Float
        var fill: dynamic
        var borderWidth: Number
    }
}

private data class UpdateContainer<T>(
    var listener: (T) -> Unit
)

@Composable
fun LineChart(
    dataSet: List<Pair<String, Number>>,
    initConfig: LineChartConfig = LineChartConfig(),
) {
    val updater = remember { UpdateContainer<List<Pair<String, Number>>> { } }

    Canvas(
        attrs = {
            ref { element ->
                val datasetState = arrayOf<ChartJs.DataSet>(
                    jso {
                        borderColor = element.accentColor
                        tension = if (initConfig.smooth) .5f else 0f
                        fill = initConfig.fill
                        borderWidth = 2
                    },
                )
                val labelState = arrayOf<String>()
                val chart = ChartJs.Chart(
                    context = element,
                    config = jso {
                        type = "line"
                        data = jso {
                            datasets = datasetState
                            labels = labelState
                        }
                        options = initConfig.asOptions
                    }
                )
                updater.listener = { data ->
                    chart.data.datasets.first().data = data.map { it.second }.toTypedArray()
                    chart.data.labels = data.map { it.first }.toTypedArray()
                    chart.update()
                }

                onDispose { element.remove() }
            }
        }
    )
    updater.listener(dataSet)
}


data class LineChartConfig(
    val xGridVisible: Boolean = false,
    val yGridVisible: Boolean = false,
    val xTicksVisible: Boolean = true,
    val yTicksVisible: Boolean = true,
    val pointsVisible: Boolean = false,
    val axisBorders: Boolean = false,
    val fill: Boolean = true,
    val smooth: Boolean = true,
)

private val LineChartConfig.asOptions: ChartJs.Options get() = jso {
    scales = jso {
        x = jso {
            grid = jso {
                display = xGridVisible
            }
            ticks = jso {
                display = xTicksVisible
            }
            border = jso {
                display = axisBorders
            }
        }
        y = jso {
            grid = jso {
                display = yGridVisible
            }
            ticks = jso {
                display = yTicksVisible
            }
            border = jso {
                display = axisBorders
            }
        }
    }
    elements = jso {
        point = jso {
            pointStyle = pointsVisible
        }
    }
    plugins = jso {
        legend = jso {
            display = false
        }
    }
}
