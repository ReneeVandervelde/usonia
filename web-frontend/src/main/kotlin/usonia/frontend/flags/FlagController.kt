package usonia.frontend.flags

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.browser.document
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import mustache.Mustache
import org.w3c.dom.Element
import org.w3c.xhr.FormData
import usonia.client.HttpClient
import usonia.frontend.ViewController
import usonia.js.addFormSubmitListener

class FlagController(
    private val client: HttpClient,
    private val logger: KimchiLogger = EmptyLogger,
): ViewController {
    private val template by lazy { document.getElementById("flag-template")!!.innerHTML }
    private val enableTemplate by lazy { document.getElementById("flag-enable-template")!!.innerHTML }
    private val disableTemplate by lazy { document.getElementById("flag-disable-template")!!.innerHTML }
    private val container by lazy { document.getElementById("flag-container") }

    override suspend fun bind() {
        val container = container ?: run {
            logger.debug("No Flags container to bind to.")
            return
        }

        container.addFormSubmitListener { data ->
            onFlagSave(data)
        }

        client.flags.collectLatest { flags ->
            flags
                .map { (key, value) -> createFlagViewModel(key, value) }
                .map {
                    when (it) {
                        is FlagViewModel.StringFlag -> Mustache.render(template, it)
                        is FlagViewModel.DisabledFlag -> Mustache.render(enableTemplate, it)
                        is FlagViewModel.EnabledFlag -> Mustache.render(disableTemplate, it)
                    }
                }
                .toList()
                .joinToString("")
                .run { container.innerHTML = this }
        }
    }

    private fun onFlagSave(data: FormData) {
        GlobalScope.launch {
            client.setFlag(data.get("flag-key"), data.get("flag-value"))
        }
    }
}
