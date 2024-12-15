package usonia.frontend.logs

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.scan
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Text
import usonia.client.HttpClient
import usonia.foundation.LogMessage
import usonia.foundation.ParameterBag
import usonia.frontend.navigation.NavigationSection
import usonia.frontend.navigation.Routing

private const val MAX_LOG_BUFFER = 5000

class LogsSection(
    private val client: HttpClient,
): NavigationSection {
    override val route: Routing = Routing.TopLevel(
        route = "/logs",
        title = "Logs",
    )

    private val logBuffer = client.bufferedLogs(MAX_LOG_BUFFER)
        .asFlow()
        .scan(listOf<LogMessage>()) { acc, value ->
            (acc + value).takeLast(MAX_LOG_BUFFER)
        }

    @Composable
    override fun renderContent(args: ParameterBag) {
        H1 { Text("Logs") }
        Div(
            attrs = {
                classes("logs-console")
            }
        ) {
            logBuffer.collectAsState(emptyList())
                .value
                .reversed()
                .forEach { LogLineLayout(it) }
        }
    }
}
