package usonia.kotlin

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

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

/**
 * Filters a boolean flow to execute only when the value is true.
 */
fun Flow<Boolean>.filterTrue(): Flow<Unit> = filter { it }.map { Unit }

/**
 * Filters a boolean flow to execute only when the value is false.
 */
fun Flow<Boolean>.filterFalse(): Flow<Unit> = filter { !it }.map { Unit }

/**
 * Map a collection flow's type to another type.
 */
fun <T, R> Flow<Collection<T>>.mapEach(mapper: (T) -> R): Flow<Collection<R>> {
    return map { it.map(mapper) }
}
