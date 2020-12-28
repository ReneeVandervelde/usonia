package usonia.js

import org.w3c.dom.Element
import org.w3c.dom.events.EventTarget

/**
 * Adds a click listener to a root element and filters with a selector.
 *
 * @receiver The parent element to observe click results in.
 * @param selector A selector string used to filter the click events.
 * @param onClick Invoked when an element in [this] matching [selector] is clicked.
 */
fun EventTarget.addElementClickListener(
    selector: String,
    onClick: (Element) -> Unit
) {
    addEventListener("click", { event ->
        val targetElement = event.target as? Element ?: return@addEventListener
        if (targetElement.matches(selector)) onClick(targetElement)
    })
}
