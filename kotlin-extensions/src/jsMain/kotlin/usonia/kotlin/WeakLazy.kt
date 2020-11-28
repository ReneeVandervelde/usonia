package usonia.kotlin

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Javascript doesn't have a weak type, so this just uses a standard lazy.
 */
actual inline fun <T> weakLazy(crossinline factory: () -> T): ReadOnlyProperty<Any, T> {
    return object: ReadOnlyProperty<Any, T> {
        val lazy by lazy { factory() }

        override fun getValue(thisRef: Any, property: KProperty<*>): T {
            return lazy
        }
    }
}
