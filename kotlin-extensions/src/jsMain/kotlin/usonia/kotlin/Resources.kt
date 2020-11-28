package usonia.kotlin

/**
 * Get the contents of a resource file as a string.
 */
actual fun Any.getResourceContents(path: String): String {
    throw NotImplementedError("Not implemented on JS")
}
