package usonia.js

import kotlinx.browser.window
import org.w3c.dom.HTMLElement

/**
 * The CSS accent color set for an element.
 */
val HTMLElement.accentColor get() = window.getComputedStyle(this).getPropertyValue("--color-accent")
