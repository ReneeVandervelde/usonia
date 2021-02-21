package usonia.core.state.memory

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toLocalDateTime
import usonia.core.state.EventAccess
import usonia.core.state.EventPublisher
import usonia.foundation.Event
import usonia.foundation.Identifier
import usonia.kotlin.datetime.ZonedClock
import usonia.kotlin.datetime.ZonedSystemClock
import kotlin.reflect.KClass
import kotlin.time.ExperimentalTime

/**
 * Provides an event read/write bus by holding events in memory.
 */
@OptIn(ExperimentalTime::class)
class InMemoryEventAccess(
    private val clock: ZonedClock = ZonedSystemClock,
    capacity: Int = 10_000,
): EventAccess, EventPublisher {
    private val eventRecords = MutableSharedFlow<Event>(replay = capacity, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val currentEvents = MutableSharedFlow<Event>()
    override val events: Flow<Event> = currentEvents
    override val eventsByDay: Flow<Map<LocalDate, Int>> = events
        .onStart { eventRecords.replayCache.firstOrNull()?.run { emit(this) } }
        .map {
            eventRecords.replayCache
                .groupBy { it.timestamp.toLocalDateTime(clock.timeZone).date }
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

    override fun temperatureHistory(devices: Collection<Identifier>): Flow<Map<Int, Float>> {
        return events
            .onStart { eventRecords.replayCache.firstOrNull()?.run { emit(this) } }
            .map {
                eventRecords.replayCache
                    .filterIsInstance<Event.Temperature>()
                    .filter { it.source in devices }
                    .let {
                        it.groupBy { (it.timestamp - clock.now()).inHours.toInt() }
                            .map { (hoursAgo, events) ->
                                hoursAgo to events.map { it.temperature }.average().toFloat()
                            }
                            .toMap()
                    }
            }
    }

    override suspend fun publishEvent(event: Event) {
        eventRecords.emit(event)
        currentEvents.emit(event)
    }
}
