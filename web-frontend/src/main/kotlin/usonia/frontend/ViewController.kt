package usonia.frontend

import kimchi.logger.KimchiLogger
import kotlinx.browser.document
import org.w3c.dom.Element

/**
 * Controller that is bound to a view.
 */
abstract class ViewController(
    protected val containerId: String,
    protected val logger: KimchiLogger,
): Controller {
    final override suspend fun onReady() {
        logger.trace { "Binding ${this::class.simpleName}" }
        val container = document.getElementById(containerId)

        onBind(container ?: run {
            logger.debug { "Unable to bind `${this::class.simpleName}` due to missing element #$containerId" }
            return
        })
    }

    abstract suspend fun onBind(element: Element)
}
