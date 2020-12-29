package usonia.core.state.memory

import kotlinx.coroutines.flow.*
import usonia.core.state.EventAccess
import usonia.core.state.EventPublisher
import usonia.foundation.Event
import usonia.foundation.Identifier
import kotlin.reflect.KClass

/**
 * Provides an event read/write bus by holding events in memory.
 */
class InMemoryEventAccess: EventAccess, EventPublisher {
    private val state = StateMap()
    private val mutableEvents = MutableSharedFlow<Event>()
    override val events: Flow<Event> = mutableEvents

    override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
        return state[id to type] as T?
    }

    override suspend fun publishEvent(event: Event) {
        state[event.source to event::class] = event
        mutableEvents.emit(event)
    }
}
