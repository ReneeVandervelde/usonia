package usonia.state

import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
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
    private val eventQueries: Lazy<EventQueries>,
    private val siteQueries: Lazy<SiteQueries>,
    private val flagQueries: Lazy<FlagQueries>,
    private val json: Json,
): DatabaseServices {
    private val eventsFlow = MutableSharedFlow<Event>()
    override val events: Flow<Event> = eventsFlow

    override val site: Flow<Site> by lazy {
        siteQueries.value
            .latest()
            .asFlow()
            .mapToOneOrNull()
            .filterNotNull()
            .map {
                it.data.let(::String).let { json.decodeFromString(Site.serializer(), it) }
            }
    }

    override val flags: Flow<Map<String, String?>> by lazy {
        flagQueries.value
            .list()
            .asFlow()
            .mapToList()
            .map {
                it.map {
                    it.id to it.data
                }.toMap()
            }
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
        return eventQueries.value.latest(type.simpleName!!, id.value) { _, _, _, data ->
            json.decodeFromString(Event.serializer(), String(data))
        }.executeAsOneOrNull() as T?
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
