package usonia.core.state

import usonia.foundation.Event

/**
 * Write access to the event bus.
 */
interface EventPublisher {
    /**
     * Send a new event out to all listeners.
     */
    suspend fun publishEvent(event: Event)
}


/**
 * Publish a collection of events one-by-one.
 */
suspend fun EventPublisher.publishAll(actions: Collection<Event>) {
    actions.forEach { publishEvent(it) }
}
