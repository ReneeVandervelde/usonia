package usonia.state

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
