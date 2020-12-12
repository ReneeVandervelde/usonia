package usonia.core.state

import kotlinx.coroutines.flow.Flow
import usonia.foundation.Event
import usonia.foundation.Uuid
import kotlin.reflect.KClass

/**
 * Provides Read access to events in the system.
 */
interface EventAccess {
    /**
     * A firehose of all events.
     */
    val events: Flow<Event>

    /**
     * Get the last known event for an item.
     */
    suspend fun <T: Event> getState(id: Uuid, type: KClass<T>): T?
}
