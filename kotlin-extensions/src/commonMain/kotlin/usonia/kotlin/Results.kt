package usonia.kotlin

import kotlinx.coroutines.CancellationException

/**
 * Re-Throw a cancellation exception if the result ended in it.
 */
fun <T> Result<T>.throwCancels(): Result<T> = apply {
    onFailure {
        if (it is CancellationException) {
            throw it
        }
    }
}
