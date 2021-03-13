package usonia.frontend.flags

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mustache.Mustache
import mustache.renderTemplate
import org.w3c.dom.Element
import org.w3c.xhr.FormData
import usonia.client.HttpClient
import usonia.frontend.ViewController
import usonia.js.addFormSubmitListener

class FlagController(
    private val client: HttpClient,
    logger: KimchiLogger = EmptyLogger,
): ViewController("flag-container", logger) {
    override suspend fun onBind(element: Element) {
        element.addFormSubmitListener { data ->
            onFlagSave(data)
        }

        client.flags.collectLatest { flags ->
            flags
                .map { (key, value) -> createFlagViewModel(key, value) }
                .map {
                    when (it) {
                        is FlagViewModel.StringFlag -> Mustache.renderTemplate("flag-template", it)
                        is FlagViewModel.DisabledFlag -> Mustache.renderTemplate("flag-enable-template", it)
                        is FlagViewModel.EnabledFlag -> Mustache.renderTemplate("flag-disable-template", it)
                    }
                }
                .toList()
                .joinToString("")
                .run { element.innerHTML = this }
        }
    }

    private fun onFlagSave(data: FormData) {
        GlobalScope.launch {
            client.setFlag(data.get("flag-key"), data.get("flag-value"))
        }
    }
}
