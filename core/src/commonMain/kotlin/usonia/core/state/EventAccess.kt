package usonia.core.state

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import usonia.foundation.Event
import usonia.foundation.PresenceState
import usonia.foundation.User
import usonia.foundation.Identifier
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
     * The number of events that occurred on each day.
     */
    val eventsByDay: Flow<Map<LocalDate, Int>>

    /**
     * Oldest recorded event, used to determine the time relevance of data.
     */
    val oldestEventTime: Flow<Instant?>

    /**
     * Get the last known event for an item.
     */
    suspend fun <T: Event> getState(id: Identifier, type: KClass<T>): T?
}

/**
 * Query whether all users in the specified list are currently away.
 */
suspend fun EventAccess.allAway(users: Collection<User>): Boolean {
    if (users.isEmpty()) return false
    return users.all { getState(it.id, Event.Presence::class)?.state == PresenceState.AWAY }
}

/**
 * Oldest recorded event, used to determine the time relevance of data.
 */
suspend fun EventAccess.getOldestEvent(): Instant? = oldestEventTime.first()
