package usonia.core.state

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import usonia.foundation.Event
import usonia.foundation.Identifier
import usonia.foundation.PresenceState
import usonia.foundation.User
import usonia.kotlin.OngoingFlow
import usonia.kotlin.first
import kotlin.reflect.KClass

/**
 * Provides Read access to events in the system.
 */
interface EventAccess {
    /**
     * A firehose of all events.
     */
    val events: OngoingFlow<Event>

    /**
     * The number of events that occurred on each day.
     */
    val eventsByDay: OngoingFlow<Map<LocalDate, Int>>

    /**
     * Oldest recorded event, used to determine the time relevance of data.
     */
    val oldestEventTime: OngoingFlow<Instant?>

    /**
     * Get the last known event for an item.
     */
    suspend fun <T: Event> getState(id: Identifier, type: KClass<T>): T?

    /**
     * History of hourly temperatures reported for a group of devices.
     *
     * Multiple reports by one or more devices are averaged into a single data point.
     *
     * @param devices The ID's of the devices to include in temperature averages.
     * @return A map of temperatures grouped by the number of hours (negative) in the past they were reported.
     */
    fun temperatureHistory(devices: Collection<Identifier>): OngoingFlow<Map<Int, Float>>

    /**
     * Get the most recent event, of any type, for a particular device.
     *
     * @param id The ID of the device to fetch an event for.
     */
    fun getLatestEvent(id: Identifier): OngoingFlow<Event>
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
