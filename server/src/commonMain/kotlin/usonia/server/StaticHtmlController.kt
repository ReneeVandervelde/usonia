package usonia.server

import usonia.kotlin.getResourceContents
import usonia.kotlin.weakLazy

/**
 * Return a static HTML response by resource.
 */
abstract class StaticHtmlController: HtmlController() {
    abstract val resource: String

    private val html: String by weakLazy {
        getResourceContents(resource)
    }

    override suspend fun getHtml(): String {
        return html
    }
}

