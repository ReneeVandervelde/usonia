package usonia.state

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.json.Json
import usonia.foundation.Event
import usonia.foundation.Identifier
import kotlin.reflect.KClass

/**
 * Implement database services via SQLite
 */
internal class DatabaseStateAccess(
    eventQueries: Lazy<EventQueries>,
    private val json: Json,
): DatabaseServices {
    private val eventQueries by eventQueries
    private val eventsFlow = MutableSharedFlow<Event>()
    override val events: Flow<Event> = eventsFlow

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
}
