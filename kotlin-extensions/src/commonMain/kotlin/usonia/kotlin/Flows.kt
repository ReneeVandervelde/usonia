package usonia.kotlin

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

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

/**
 * Map a collection flow's items to another type, catching any errors thrown.
 *
 * @receiver A flow that emits collections whose items are to be transformed
 * @param transformer Transformation action to perform on each item in the collections emitted by the flow.
 * @return A flow of collections whose items are a result of the transformation action.
 */
inline fun <T, R> Flow<Iterable<T>>.mapEachCatching(crossinline transformer: (T) -> R): Flow<List<Result<R>>> {
    return map { items ->
        items.map { item ->
            runCatching { transformer(item) }
        }
    }
}

/**
 * Run an action for each result that is a failure in an emitted collection.
 *
 * @receiver A flow that emits collections of results
 * @param action An action to run for every failed result in each emitted collection
 * @return The unmodified receiver flow
 */
inline fun <T> Flow<Iterable<Result<T>>>.onEachFailure(crossinline action: (Throwable) -> Unit): Flow<Iterable<Result<T>>> {
    return onEach {
        it.forEach {
            it.onFailure { action(it) }
        }
    }
}

/**
 * Filter the items in a collection of results to only successful results.
 *
 * @receiver A flow that emits collections of results
 * @return A flow of collections that contain the results of only successful items emitted by the receiver.
 */
fun <T> Flow<Iterable<Result<T>>>.filterSuccess(): Flow<List<T>> {
    return map { it.filter { it.isSuccess }.map { it.getOrThrow() } }
}
