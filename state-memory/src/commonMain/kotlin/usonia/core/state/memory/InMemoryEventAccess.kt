package usonia.core.state.memory

import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import usonia.core.state.ConfigurationAccess
import usonia.core.state.EventAccess
import usonia.core.state.EventPublisher
import usonia.foundation.Event
import usonia.foundation.Identifier
import kotlin.reflect.KClass

/**
 * Provides an event read/write bus by holding events in memory.
 */
class InMemoryEventAccess(
    private val configurationAccess: ConfigurationAccess
): EventAccess, EventPublisher {
    private val state = StateMap()
    private val eventChannel = BroadcastChannel<Event>(Channel.BUFFERED)
    override val events: Flow<Event> = eventChannel.asFlow()

    override suspend fun <T : Event> getState(id: Identifier, type: KClass<T>): T? {
        return state[id to type] as T?
    }

    override suspend fun publishEvent(event: Event) {
        state[event.source to event::class] = event
        eventChannel.send(event)
    }
}
