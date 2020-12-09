package usonia.kotlin

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Emits a single item and awaits cancellation.
 *
 * This is similar to [kotlinx.coroutines.flow.flowOf] but does not complete
 * after the initial item is returned, but instead suspends until cancelled.
 */
fun <T> suspendedFlow(single: T): Flow<T> = flow {
    emit(single)
    awaitCancellation()
}
