package usonia.frontend.logs

import kotlinx.browser.document
import kotlinx.coroutines.flow.collect
import mustache.Mustache
import usonia.client.HttpClient
import usonia.frontend.ViewController

/**
 * Displays live server logs.
 */
class LogController(
    private val client: HttpClient,
): ViewController {
    private val template by lazy { document.getElementById("log-message-template")?.innerHTML!! }
    private val logs by lazy { document.getElementById("logs") }

    override suspend fun bind() {
        client.logs.collect {
            logs?.innerHTML += Mustache.render(template, it)
        }
    }
}
