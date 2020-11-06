package usonia.foundation

import java.util.*

actual fun createUuid(): Uuid = Uuid(UUID.randomUUID().toString())
