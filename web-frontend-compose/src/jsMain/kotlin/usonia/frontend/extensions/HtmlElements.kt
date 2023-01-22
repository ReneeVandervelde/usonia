package usonia.frontend.extensions

import kotlinx.browser.window
import org.w3c.dom.HTMLElement

/**
 * Derive the window accent color defined for an element.
 */
val HTMLElement.accentColor get() = window.getComputedStyle(this).getPropertyValue("--color-accent")
