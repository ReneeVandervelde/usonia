package usonia.frontend.logs

import androidx.compose.runtime.*
import kotlinx.coroutines.flow.scan
import org.jetbrains.compose.web.dom.Div
import usonia.client.HttpClient
import usonia.foundation.LogMessage
import usonia.frontend.navigation.NavigationSection

private const val MAX_LOG_BUFFER = 5000

class LogsSection(
    private val client: HttpClient,
): NavigationSection {
    override val title: String = "Logs"
    private val logBuffer = client.bufferedLogs(MAX_LOG_BUFFER)
        .asFlow()
        .scan(listOf<LogMessage>()) { acc, value ->
            (acc + value).takeLast(MAX_LOG_BUFFER)
        }

    @Composable
    override fun renderContent() {
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
