package usonia.kotlin

/**
 * Get the contents of a resource file as a string.
 */
expect fun Any.getResourceContents(path: String): String
