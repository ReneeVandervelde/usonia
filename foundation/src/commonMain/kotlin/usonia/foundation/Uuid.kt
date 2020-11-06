package usonia.foundation

inline class Uuid(val value: String)

expect fun createUuid(): Uuid
