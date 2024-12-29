package usonia.core.state

import com.inkapplications.coroutines.ongoing.OngoingFlow
import com.inkapplications.coroutines.ongoing.first
import com.inkapplications.coroutines.ongoing.map
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import usonia.foundation.*
import kotlin.reflect.KClass
import kotlin.time.Duration

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
     * Latest events for a specific device.
     *
     * @param id The device to get event data for
     * @param size The maximum number of events to return in a single set.
     *        If unspecified, the default size will be determined by the
     *        data implementation.
     * @return A list of the most recent events for a device.
     */
    fun deviceEventHistory(id: Identifier, size: Int? = null): OngoingFlow<List<Event>>

    /**
     * The number of events of a specific category that have occurred for a device.
     */
    fun eventCount(id: Identifier, category: EventCategory): OngoingFlow<Long>

    /**
     * History of hourly temperatures reported for a group of devices.
     *
     * Multiple reports by one or more devices are averaged into a single data point.
     *
     * @param devices The ID's of the devices to include in temperature averages.
     * @param limit a maximum amount of time to search for events in history.
     *        If unspecified, the default limit will be determined by the data
     *        implementation.
     * @return A map of temperatures grouped by the number of hours (negative) in the past they were reported.
     */
    fun temperatureHistorySnapshots(devices: Collection<Identifier>, limit: Duration? = null): OngoingFlow<List<TemperatureSnapshot>>

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

/**
 * Observe the recent temperature history of a room based on its devices.
 *
 * @param room The room to get temperature info for, based on its associated
 *        devices. Note that some devices, such as a refrigerator are excluded
 *        from this list.
 * @param range The amount of time to fetch history for.
 * @return A flow of temperature data that is divided into hours snapshots.
 */
fun EventAccess.roomTemperatureHistory(room: Room, range: Duration? = null): OngoingFlow<List<TemperatureSnapshot>> {
    val excludedTypes = setOf(Fixture.Refrigerator, Fixture.Freezer)
    return room.devices
        .filter { Event.Temperature::class in it.capabilities.events }
        .filter { it.fixture !in excludedTypes }
        .map { it.id }
        .let { temperatureHistorySnapshots(it, range) }
        .map { it.toList() }
}
