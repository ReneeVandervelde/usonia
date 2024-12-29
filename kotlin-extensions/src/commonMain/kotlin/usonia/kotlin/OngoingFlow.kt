package usonia.kotlin

import com.inkapplications.coroutines.ongoing.OngoingFlow
import com.inkapplications.coroutines.ongoing.unsafeTransform
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.scan

/**
 * @see filterTrue
 */
fun OngoingFlow<Boolean>.filterTrue() = unsafeTransform { filterTrue() }

/**
 * Scans a flow's emissions by grouping them into a buffer of a specified size.
 *
 * ie. given the flow: `[A, B, C, D, E]`
 * calling `rollingWindow(3)` will produce: `[[A, B, C], [B, C, D], [C, D, E]]`
 */
fun <T> OngoingFlow<T>.rollingWindow(size: Int): OngoingFlow<List<T>> = unsafeTransform {
    scan(emptyList<T>()) { accumulator, value: T ->
        (accumulator + value).takeLast(size)
    }.filter { it.size == size }
}
