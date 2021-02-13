package usonia.core.state.memory

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import usonia.core.state.EventAccess
import usonia.core.state.EventPublisher
import usonia.foundation.Event
import usonia.foundation.Identifier
import kotlin.reflect.KClass

/**
 * Provides an event read/write bus by holding events in memory.
 */
class InMemoryEventAccess(
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    capacity: Int = 10_000,
): EventAccess, EventPublisher {
    private val eventRecords = MutableSharedFlow<Event>(replay = capacity, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val currentEvents = MutableSharedFlow<Event>()
    override val events: Flow<Event> = currentEvents
    override val eventsByDay: Flow<Map<LocalDate, Int>> = events
        .onStart { eventRecords.replayCache.firstOrNull()?.run { emit(this) } }
        .map {
            eventRecords.replayCache
                .groupBy { it.timestamp.toLocalDateTime(timeZone).date }
                .mapValues { it.value.size }
        }
    override val oldestEventTime = eventRecords
        .map {
            eventRecords.replayCache.minByOrNull { it.timestamp }?.timestamp
        }
        .distinctUntilChanged()

    override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
        return eventRecords.replayCache
            .lastOrNull { it.source == id && it::class == type } as T?
    }

    override suspend fun publishEvent(event: Event) {
        eventRecords.emit(event)
        currentEvents.emit(event)
    }
}
