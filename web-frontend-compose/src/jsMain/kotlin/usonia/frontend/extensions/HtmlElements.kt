package usonia.frontend.extensions

import androidx.compose.runtime.Composable
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.AttrBuilderContext
import org.jetbrains.compose.web.dom.ContentBuilder
import org.jetbrains.compose.web.dom.TagElement
import org.w3c.dom.HTMLElement

/**
 * Derive the window accent color defined for an element.
 */
val HTMLElement.accentColor get() = window.getComputedStyle(this).getPropertyValue("--color-accent")

/**
 * Compose is missing the strong html element.
 */
@Composable
fun Strong(
    attrs: AttrBuilderContext<HTMLElement>? = null,
    content: ContentBuilder<HTMLElement>? = null
) = TagElement("strong", applyAttrs = attrs, content = content)
