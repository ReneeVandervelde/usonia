package usonia.frontend.logs

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import usonia.foundation.LogMessage

@Composable
fun LogLineLayout(log: LogMessage) {
    Div(
        attrs = {
            classes("log-message")
            attr("data-level", log.level.name)
        }
    ) {
        Text("[")
        Span(
            attrs = {
                classes("log-level-label")
            }
        ) {
            Text(log.level.name)
        }
        Text("]: ")
        Text(log.message)

        val trace = log.stackTrace
        if (trace != null) {
            Div(
                attrs = {
                    classes("log-message-stacktrace")
                }
            ) {
                Text(trace)
            }
        }
    }
}
