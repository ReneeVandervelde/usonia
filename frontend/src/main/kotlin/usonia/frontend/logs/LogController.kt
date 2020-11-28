package usonia.frontend.logs

import kotlinx.browser.document
import kotlinx.coroutines.flow.collect
import usonia.client.UsoniaClient
import usonia.frontend.ViewController

/**
 * Displays live server logs.
 */
class LogController(
    private val client: UsoniaClient,
): ViewController {
    private val logs by lazy { document.getElementById("logs") }

    override suspend fun bind() {
        client.logs.collect {
            logs?.innerHTML += "$it\n"
        }
    }
}
