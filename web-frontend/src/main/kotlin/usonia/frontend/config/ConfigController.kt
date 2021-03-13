package usonia.frontend.config

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.flow.*
import mustache.Mustache
import mustache.renderTemplate
import org.w3c.dom.Element
import usonia.client.HttpClient
import usonia.frontend.ViewController

/**
 * Lists out the configuration of the current site.
 */
class ConfigController(
    private val client: HttpClient,
    logger: KimchiLogger = EmptyLogger,
): ViewController("config", logger) {
    override suspend fun onBind(element: Element) {
        client.site
            .onEach { logger.trace("New Config Loaded") }
            .map { ConfigViewModel(it) }
            .collect { configViewModel ->
                element.innerHTML = Mustache.renderTemplate("config-template", configViewModel)
            }
    }
}
