package usonia.js

import org.w3c.dom.Element
import org.w3c.dom.HTMLFormElement
import org.w3c.dom.events.EventTarget
import org.w3c.xhr.FormData

/**
 * Adds a click listener to a root element and filters with a selector.
 *
 * @receiver The parent element to observe click results in.
 * @param selector A selector string used to filter the click events.
 * @param onClick Invoked when an element in [this] matching [selector] is clicked.
 */
fun EventTarget.addElementClickListener(
    selector: String,
    onClick: (Element) -> Unit,
) {
    addEventListener("click", { event ->
        event.preventDefault()
        val targetElement = event.target as? Element ?: return@addEventListener
        if (targetElement.matches(selector)) onClick(targetElement)
    })
}

/**
 * Add a listener for form submissions on a container.
 *
 * @receiver The parent element to observe for form submissions.
 * @param selector Selector string used to filter forms
 * @param onClick Invoked when a form in [this] matching [selector] is submitted.
 */
fun EventTarget.addFormSubmitListener(
    selector: String? = null,
    onClick: (FormData) -> Unit,
) {
    addEventListener("submit", { event ->
        event.preventDefault()
        val targetElement = event.target as? HTMLFormElement ?: return@addEventListener
        if (selector?.let { targetElement.matches(it) } == false) return@addEventListener

        val formData = FormData(targetElement)
        onClick(formData)
    })
}
