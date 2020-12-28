package usonia.frontend.config

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.browser.document
import kotlinx.coroutines.flow.*
import mustache.Mustache
import usonia.client.HttpClient
import usonia.frontend.ViewController

/**
 * Lists out the configuration of the current site.
 */
class ConfigController(
    private val client: HttpClient,
    private val logger: KimchiLogger = EmptyLogger,
): ViewController {
    private val template by lazy { document.getElementById("config-template")?.innerHTML }
    private val config by lazy { document.getElementById("config") }

    override suspend fun bind() {
        client.site
            .onEach { logger.trace("New Config Loaded") }
            .map { ConfigViewModel(it) }
            .collect { configViewModel ->
                config?.innerHTML = template?.let { Mustache.render(it, configViewModel) }.orEmpty()
            }
    }
}
