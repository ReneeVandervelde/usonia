package usonia.kotlin

/**8
 * Map a collection to a typed array.
 */
inline fun <T, reified R> Collection<T>.mapArray(mapping: (T) -> R): Array<R> {
    return map(mapping).toTypedArray()
}
