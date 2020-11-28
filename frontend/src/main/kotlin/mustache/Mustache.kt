package mustache

@JsModule("Mustache")
external object Mustache {
    fun render(template: String, data: Any): String
}
