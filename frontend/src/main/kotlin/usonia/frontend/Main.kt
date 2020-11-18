package usonia.frontend

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import usonia.client.UsoniaClient

fun main() {
    console.log("Starting Application")
    GlobalScope.launch {
        UsoniaClient(
            window.location.host,
            window.location.port.takeIf { it.isNotEmpty() }?.toInt() ?: 80
        ).logs.collect {
            document.getElementById("logs")?.innerHTML += "$it\n"
        }
    }
}
