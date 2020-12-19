package usonia.core.state

import kotlinx.coroutines.flow.Flow
import usonia.foundation.Event
import usonia.foundation.PresenceState
import usonia.foundation.User
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

/**
 * Query whether all users in the specified list are currently away.
 */
suspend fun EventAccess.allAway(users: Collection<User>): Boolean {
    if (users.isEmpty()) return false
    return users.all { getState(it.id, Event.Presence::class)?.state == PresenceState.AWAY }
}
