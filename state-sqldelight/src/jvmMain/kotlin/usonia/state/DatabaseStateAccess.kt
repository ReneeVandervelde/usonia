package usonia.state

import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrNull
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import usonia.foundation.Event
import usonia.foundation.EventSerializer
import usonia.foundation.Identifier
import usonia.foundation.Site
import usonia.kotlin.OngoingFlow
import usonia.kotlin.asOngoing
import usonia.kotlin.datetime.ZonedClock
import usonia.kotlin.datetime.ZonedSystemClock
import usonia.kotlin.mapEach
import kotlin.reflect.KClass
import kotlin.time.ExperimentalTime

/**
 * Implement database services via SQLite
 */
@OptIn(ExperimentalTime::class)
internal class DatabaseStateAccess(
    private val eventQueries: Lazy<EventQueries>,
    private val siteQueries: Lazy<SiteQueries>,
    private val flagQueries: Lazy<FlagQueries>,
    private val json: Json,
    private val zonedClock: ZonedClock = ZonedSystemClock,
): DatabaseServices {
    private val eventsFlow = MutableSharedFlow<Event>()
    override val events: OngoingFlow<Event> = eventsFlow.asOngoing()
    override val eventsByDay: OngoingFlow<Map<LocalDate, Int>> by lazy {
        eventQueries.value.eventsByDay()
            .asFlow()
            .mapToList()
            .map {
                it.map {
                    LocalDate.parse(it.localdate) to it.total.toInt()
                }.toMap()
            }
            .asOngoing()
    }

    override val oldestEventTime: OngoingFlow<Instant?> by lazy {
        eventQueries.value
            .oldestEvent()
            .asFlow()
            .mapToOneOrNull()
            .map {
                it?.let { Instant.fromEpochMilliseconds(it) }
            }
            .asOngoing()
    }

    override val site: OngoingFlow<Site> by lazy {
        siteQueries.value
            .latest()
            .asFlow()
            .mapToOneOrNull()
            .filterNotNull()
            .map {
                it.data.let(::String).let { json.decodeFromString(Site.serializer(), it) }
            }
            .asOngoing()
    }

    override val flags: OngoingFlow<Map<String, String?>> by lazy {
        flagQueries.value
            .list()
            .asFlow()
            .mapToList()
            .map {
                it.map {
                    it.id to it.data
                }.toMap()
            }
            .asOngoing()
    }

    override suspend fun publishEvent(event: Event) {
        eventsFlow.emit(event)
        eventQueries.value.insert(
            timestamp = event.timestamp.toEpochMilliseconds(),
            source = event.source.value,
            type = event::class.simpleName!!,
            data = json.encodeToString(Event.serializer(), event).toByteArray(),
        )
    }

    override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
        return eventQueries.value.latest(type.simpleName!!, id.value) { _, _, _, _, data ->
            json.decodeFromString(Event.serializer(), String(data))
        }.executeAsOneOrNull() as T?
    }

    override fun temperatureHistory(devices: Collection<Identifier>): OngoingFlow<Map<Int, Float>> {
         return devices
             .map { it.value }
             .let { eventQueries.value.eventsBySourceAndType(it, Event.Temperature::class.simpleName!!) }
             .asFlow()
             .mapToList()
             .mapEach { json.decodeFromString(EventSerializer, String(it)) as Event.Temperature }
             .map {
                 it.groupBy { (it.timestamp - zonedClock.now()).inHours.toInt() }
                     .map { (hoursAgo, events) ->
                         hoursAgo to events.map { it.temperature }.average().toFloat()
                     }
                     .toMap()
             }
             .asOngoing()
    }

    override suspend fun updateSite(site: Site) {
        siteQueries.value.update(json.encodeToString(Site.serializer(), site).toByteArray())
    }

    override suspend fun setFlag(key: String, value: String?) {
        flagQueries.value.update(key, value)
    }

    override suspend fun removeFlag(key: String) {
        flagQueries.value.delete(key)
    }
}
