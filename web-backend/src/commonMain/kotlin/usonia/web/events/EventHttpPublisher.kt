package usonia.web.events

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.serialization.json.Json
import usonia.core.server.HttpRequest
import usonia.core.server.RestController
import usonia.core.server.RestResponse
import usonia.core.state.EventPublisher
import usonia.foundation.Event
import usonia.foundation.EventSerializer
import usonia.foundation.Status
import usonia.foundation.Statuses

/**
 * Publishes raw event data.
 */
internal class EventHttpPublisher(
    private val eventPublisher: EventPublisher,
    json: Json = Json,
    logger: KimchiLogger = EmptyLogger
): RestController<Event, Status>(json, logger) {
    override val method: String = "POST"
    override val path: String = "/events"
    override val deserializer = EventSerializer
    override val serializer = Status.serializer()

    override suspend fun getResponse(data: Event, request: HttpRequest): RestResponse<Status> {
        eventPublisher.publishEvent(data)
        return RestResponse(Statuses.SUCCESS)
    }
}
