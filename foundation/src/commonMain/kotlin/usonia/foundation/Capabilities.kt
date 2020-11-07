package usonia.foundation

import kotlin.reflect.KClass

/**
 * Describes the Actions and Events that a device is capable of.
 */
data class Capabilities(
    val actions: Set<KClass<out Action>> = emptySet(),
    val events: Set<KClass<out Event>> = emptySet()
)
