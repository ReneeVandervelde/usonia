package usonia.chart

import org.w3c.dom.HTMLCanvasElement

@JsModule("chart.js")
external class Chart(
    context: HTMLCanvasElement,
    config: Config,
) {
    var data: Data

    fun update()
}

data class Config(
    var data: Data,
    var type: Type = Type.line,
    var options: Options? = Options(),
)

data class Options(
    var responsive: Boolean = true,
    var maintainAspectRatio: Boolean = false,
    var scales: ScaleOptions? = null,
    var legend: LegendOptions? = null,
)

data class LegendOptions(
    var display: Boolean? = null,
)

data class ScaleOptions(
    var xAxes: Array<AxisOptions>,
    var yAxes: Array<AxisOptions>,
)

data class AxisOptions(
    var gridLines: GridLineOptions? = null,
    var ticks: TickOptions = TickOptions(),
)

data class GridLineOptions(
    var display: Boolean? = null,
)

data class TickOptions(
    var precision: Int = 0,
)

enum class Type {
    line,
}

data class Data(
    var datasets: Array<DataSet> = arrayOf(),
    var labels: Array<String> = arrayOf(),
)

data class DataSet(
    var label: String,
    var data: Array<Int> = emptyArray(),
    var borderColor: String? = null,
    var borderWidth: Int = 1,
    var pointRadius: Int? = null,
)
