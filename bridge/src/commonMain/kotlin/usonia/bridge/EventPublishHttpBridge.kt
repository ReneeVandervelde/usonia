package usonia.bridge

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.serialization.KSerializer
import usonia.core.server.*
import usonia.foundation.Event
import usonia.serialization.EventSerializer
import usonia.state.EventPublisher

/**
 * Sends deserialized Events to the event publisher.
 */
class EventPublishHttpBridge(
    private val eventPublisher: EventPublisher,
    logger: KimchiLogger = EmptyLogger
): RestController<Event, StatusResponse>(logger) {
    override val method: String = "POST"
    override val path: String = "/events"
    override val deserializer: KSerializer<Event> = EventSerializer
    override val serializer: KSerializer<StatusResponse> = StatusResponse.serializer()

    override suspend fun getResponse(data: Event, request: HttpRequest): RestResponse<StatusResponse> {
        eventPublisher.publishEvent(data)
        return SUCCESS
    }
}

