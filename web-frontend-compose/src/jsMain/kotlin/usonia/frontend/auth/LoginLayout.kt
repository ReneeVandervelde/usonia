package usonia.frontend.auth

import androidx.compose.runtime.*
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.attributes.name
import org.jetbrains.compose.web.dom.*

/**
 * Basic authentication input to set the PSK.
 */
@Composable
fun LoginLayout(
    authentication: WebAuthentication,
) {
    Header(
        attrs = {
            classes("content-break")
        }
    ) {
        H1 {
            Text("Login")
        }
    }
    Section(
        attrs = {
            classes("content-break")
        }
    ) {
        var psk by remember { mutableStateOf("") }
        Label {
            Text("PSK")
        }
        Input(
            type = InputType.Password,
            attrs = {
                name("psk")
                onChange {
                    psk = it.value
                }
            }
        )
        Br()
        Button(
            attrs = {
                onClick {
                    authentication.login(psk)
                }
            }
        ) {
            Text("Login")
        }
    }
}

