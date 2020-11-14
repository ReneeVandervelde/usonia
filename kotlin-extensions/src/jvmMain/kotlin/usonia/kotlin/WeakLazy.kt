package usonia.kotlin

import java.lang.ref.WeakReference
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

actual inline fun <T> weakLazy(crossinline factory: () -> T): ReadOnlyProperty<Any, T> {
    return object: ReadOnlyProperty<Any, T> {
        var weakRef = WeakReference<T>(null)

        override fun getValue(thisRef: Any, property: KProperty<*>): T {
            return weakRef.get() ?: factory().also {
                weakRef = WeakReference(it)
            }
        }
    }
}
