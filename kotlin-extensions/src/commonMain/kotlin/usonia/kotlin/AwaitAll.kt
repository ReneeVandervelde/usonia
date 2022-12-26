package usonia.kotlin

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.awaitAll

/**
 * Map each action to a deferred job and await completion.
 */
suspend inline fun <T> Collection<T>.awaitAll(action: (T) -> Deferred<Any>) {
    map { action(it) }.awaitAll()
}
