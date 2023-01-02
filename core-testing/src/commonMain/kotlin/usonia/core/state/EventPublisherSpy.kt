package usonia.core.state

import usonia.foundation.Event

class EventPublisherSpy: EventPublisher {
    val events = mutableListOf<Event>()

    override suspend fun publishEvent(event: Event) {
        events += event
    }
}
