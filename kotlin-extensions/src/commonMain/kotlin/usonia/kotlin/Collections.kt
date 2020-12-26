package usonia.kotlin

/**8
 * Map a collection to a typed array.
 */
inline fun <T, reified R> Collection<T>.mapArray(mapping: (T) -> R): Array<R> {
    return map(mapping).toTypedArray()
}

/**
 * Match a single element or throw an exception with [message]
 */
inline fun <T> Iterable<T>.singleOrThrow(message: String, predicate: (T) -> Boolean): T {
    return try {
        single(predicate)
    } catch (e: Throwable) {
        throw IllegalArgumentException(message, e)
    }
}

/**
 * Map a set's values from [T] to [R]
 */
inline fun <T, R> Set<T>.mapSet(mapping: (T) -> R): Set<R> = map(mapping).toSet()
