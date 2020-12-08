package mustache

@JsModule("mustache")
external object Mustache {
    fun render(template: String, data: Any): String
}
