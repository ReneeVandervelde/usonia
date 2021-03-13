package mustache

import kotlinx.browser.document

@JsModule("mustache")
external object Mustache {
    fun render(template: String, data: Any): String
}

/**
 * Render a template element in the document.
 *
 * @param id The ID of the template to use for rendering.
 * @param data Template viewmodel to use for data when rendering.
 * @return Rendered template HTML.
 */
fun Mustache.renderTemplate(id: String, data: Any): String {
    val template = document.getElementById(id)?.innerHTML
        ?: throw IllegalArgumentException("No template with ID: `$id`")

    return Mustache.render(template, data)
}
