package usonia.foundation

@JsModule("uuid")
@JsNonModule
external fun v4(): String

/**
 * Generate a random UUID.
 */
actual fun createUuid(): Uuid = Uuid(v4())
