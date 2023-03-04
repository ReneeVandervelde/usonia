package usonia.frontend.widgets

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Text
import usonia.frontend.extensions.Strong

/**
 * A Label and value on a single line.
 */
@Composable
fun KeyValue(
    key: String,
    value: String,
) {
    Strong {
        Text("$key: ")
    }
    Text(value)
}
