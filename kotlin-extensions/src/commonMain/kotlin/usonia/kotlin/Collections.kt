package usonia.kotlin

/**
 * Match a single element or throw an exception with [message]
 */
inline fun <T> Iterable<T>.singleOrThrow(message: String, predicate: (T) -> Boolean): T {
    return try {
        single(predicate)
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException(message, e)
    } catch (e: NoSuchElementException) {
        throw IllegalArgumentException(message, e)
    }
}
