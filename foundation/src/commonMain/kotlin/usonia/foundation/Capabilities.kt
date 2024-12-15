package usonia.foundation

import kotlin.reflect.KClass
import kotlin.time.Duration

/**
 * Describes the Actions and Events that a device is capable of.
 */
data class Capabilities(
    val archetypeId: String? = null,
    val actions: Set<KClass<out Action>> = emptySet(),
    val events: Set<KClass<out Event>> = emptySet(),
    val heartbeat: Duration? = null,
)
