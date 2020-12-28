package usonia.kotlin

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Scope with an unconfined context.
 *
 * This allows new coroutines to be launched on the current thread without
 * blocking the current method.
 */
fun UnconfinedScope() = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())

/**
 * Scope with an IO Dispatcher if the platform supports it.
 *
 * If the platform does not support an IO dispatcher, such as javascript,
 * this should use the default dispatcher.
 */
expect fun IoScope(): CoroutineScope
