package usonia.kotlin

/**
 * Run a side effect only if [this] is null.
 */
inline fun <T: Any?> T.alsoIfNull(operation: () -> Unit): T {
    return this.also { if(this == null) operation() }
}
