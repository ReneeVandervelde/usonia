package usonia.kotlin

import kotlin.properties.ReadOnlyProperty

/**
 * Delegate property that is loaded lazily and held as a weak reference.
 *
 * This should be used for values that are expensive to load and to hold in
 * memory.
 */
expect inline fun <T> weakLazy(crossinline factory: () -> T): ReadOnlyProperty<Any, T>
