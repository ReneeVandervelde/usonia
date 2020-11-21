package usonia.kotlin

/**
 * Ensures that a block of code never returns by throwing an exception if it does.
 *
 * @throws IllegalStateException if the block returns.
 */
suspend inline fun neverEnding(crossinline action: suspend () -> Unit): Nothing {
    action()
    throw IllegalStateException("Unexpected end of never-ending operation")
}
