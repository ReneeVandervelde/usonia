package usonia.hubitat

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.serialization.KSerializer
import usonia.core.server.*
import usonia.core.state.EventPublisher
import usonia.foundation.Event
import usonia.foundation.Status
import usonia.foundation.Statuses.SUCCESS
import usonia.serialization.EventSerializer
import usonia.serialization.StatusSerializer

/**
 * Sends deserialized Events to the event publisher.
 */
internal class EventPublishHttpBridge(
    private val eventPublisher: EventPublisher,
    logger: KimchiLogger = EmptyLogger
): RestController<Event, Status>(logger) {
    override val method: String = "POST"
    override val path: String = "/events"
    override val deserializer: KSerializer<Event> = EventSerializer
    override val serializer: KSerializer<Status> = StatusSerializer

    override suspend fun getResponse(data: Event, request: HttpRequest): RestResponse<Status> {
        eventPublisher.publishEvent(data)
        return RestResponse(SUCCESS)
    }
}

