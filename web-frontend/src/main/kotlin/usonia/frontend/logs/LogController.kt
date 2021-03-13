package usonia.frontend.logs

import kimchi.logger.KimchiLogger
import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import mustache.Mustache
import mustache.renderTemplate
import org.w3c.dom.Element
import org.w3c.dom.get
import usonia.client.HttpClient
import usonia.foundation.LogMessage
import usonia.frontend.ViewController
import usonia.js.addElementClickListener
import usonia.kotlin.DefaultScope

/**
 * Displays live server logs.
 */
class LogController(
    private val client: HttpClient,
    logger: KimchiLogger,
): ViewController("log", logger), CoroutineScope by DefaultScope() {
    private val console by lazy { document.getElementById("log-console") }
    private val actions by lazy { document.getElementById("log-actions") }

    override suspend fun onBind(element: Element) {
        actions?.addElementClickListener("button[name='log-load']") {
            when (it.attributes["value"]?.value) {
                "PART" -> launch { loadPartialLogs() }
                "FULL" -> launch { loadFullLogs() }
                else -> logger.error("Unknown load value")
            }
            actions?.remove()
        }
    }

    private suspend fun loadPartialLogs() {
        client.bufferedLogs(20).collect {
            flushLog(it)
        }
    }

    private suspend fun loadFullLogs() {
        client.bufferedLogs().collect {
            flushLog(it)
        }
    }

    private fun flushLog(log: LogMessage) {
        console?.insertAdjacentHTML("afterBegin", Mustache.renderTemplate("log-message-template", log))
    }
}
