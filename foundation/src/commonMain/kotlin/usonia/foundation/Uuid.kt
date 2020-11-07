package usonia.foundation

/**
 * A Universally unique Identifier String.
 *
 * @see createUuid to generate.
 */
inline class Uuid(val value: String)

/**
 * Generate a random UUID.
 */
expect fun createUuid(): Uuid
