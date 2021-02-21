package usonia.js

import kotlinx.browser.document
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.Element
import org.w3c.dom.MutationObserver
import org.w3c.dom.MutationObserverInit
import kotlin.coroutines.resume

/**
 * Suspend until an element appears in the DOM
 *
 * @param id The ID of the element to look for.
 * @return The added element object.
 */
suspend fun awaitElement(id: String) = suspendCancellableCoroutine<Element> { continuation ->
    val observer = MutationObserver { record, observer ->
        document.getElementById(id)?.run {
            continuation.resume(this)
            observer.disconnect()
        }
    }

    continuation.invokeOnCancellation { observer.disconnect() }
    observer.observe(document, MutationObserverInit(attributes = false, childList = true, characterData = false, subtree = true))
}
