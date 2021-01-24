package usonia.state

import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToOneOrNull
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.Json
import usonia.foundation.Event
import usonia.foundation.Identifier
import usonia.foundation.Site
import kotlin.reflect.KClass

/**
 * Implement database services via SQLite
 */
internal class DatabaseStateAccess(
    eventQueries: Lazy<EventQueries>,
    siteQueries: Lazy<SiteQueries>,
    private val json: Json,
): DatabaseServices {
    private val eventQueries by eventQueries
    private val siteQueries by siteQueries
    private val eventsFlow = MutableSharedFlow<Event>()
    override val events: Flow<Event> = eventsFlow

    override val site: Flow<Site> = siteQueries.value
        .latest()
        .asFlow()
        .mapToOneOrNull()
        .filterNotNull()
        .map {
            it.data.let(::String).let { json.decodeFromString(Site.serializer(), it) }
        }

    override suspend fun publishEvent(event: Event) {
        eventsFlow.emit(event)
        eventQueries.insert(
            timestamp = event.timestamp.toEpochMilliseconds(),
            source = event.source.value,
            type = event::class.simpleName!!,
            data = json.encodeToString(Event.serializer(), event).toByteArray(),
        )
    }

    override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
        return eventQueries.latest(type.simpleName!!, id.value) { _, _, _, data ->
            json.decodeFromString(Event.serializer(), String(data))
        }.executeAsOneOrNull() as T?
    }

    override suspend fun updateSite(site: Site) {
        siteQueries.update(json.encodeToString(Site.serializer(), site).toByteArray())
    }
}
