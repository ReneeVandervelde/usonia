package usonia.frontend.extensions

fun <T : Any> jso(): T = js("({})")
fun <T : Any> jso(block: T.() -> Unit): T = jso<T>().apply(block)
