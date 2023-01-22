package usonia.frontend.flags

import androidx.compose.runtime.*
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.onSubmit
import org.jetbrains.compose.web.dom.*

@Composable
fun DisabledFlagControl(
    key: String,
    onFlagChange: (String?) -> Unit,
) {
    InlineForm {
        Label {
            Text("$key:")
        }
        Button(
            attrs = {
                onClick {
                    onFlagChange("true")
                }
            }
        ) {
            Text("Off")
        }
    }
}

@Composable
fun EnabledFlagControl(
    key: String,
    onFlagChange: (String?) -> Unit,
) {
    InlineForm {
        Label {
            Text("$key:")
        }
        Button(
            attrs = {
                classes("primary")
                onClick {
                    onFlagChange("false")
                }
            }
        ) {
            Text("On")
        }
    }
}

@Composable
fun TextFlagField(
    key: String,
    value: String,
    onFlagChange: (String?) -> Unit,
) {
    var currentValue by remember { mutableStateOf(value) }
    InlineForm {
        Label {
            Text("$key:")
        }
        Input(
            type = InputType.Text,
        ) {
            value(currentValue)
            onInput {
                currentValue = it.value
            }
        }
        Button(
            attrs = {
                onClick {
                    onFlagChange(currentValue)
                }
            }
        ) {
            Text("Save")
        }
    }
}

@Composable
private fun InlineForm(fields: @Composable () -> Unit) {
    Form(
        attrs = {
            classes("inline")
            onSubmit { it.preventDefault() }
        },
    ) {
        fields()
    }
}
