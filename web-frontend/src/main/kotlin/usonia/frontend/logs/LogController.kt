package usonia.frontend.logs

import kimchi.logger.KimchiLogger
import kotlinx.browser.document
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import mustache.Mustache
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
    private val logger: KimchiLogger,
): ViewController, CoroutineScope by DefaultScope() {
    private val container by lazy { document.getElementById("log") }
    private val template by lazy { document.getElementById("log-message-template")?.innerHTML }
    private val console by lazy { document.getElementById("log-console") }
    private val actions by lazy { document.getElementById("log-actions") }

    override suspend fun bind() {
        if (template == null || container == null || console == null) {
            logger.warn("No Log Container found")
            return
        }

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
        console?.insertAdjacentHTML("afterBegin", Mustache.render(template!!, log))
    }
}
