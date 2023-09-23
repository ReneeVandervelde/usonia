package usonia.web.events

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.serialization.json.Json
import usonia.foundation.Event
import usonia.foundation.EventSerializer
import usonia.foundation.Status
import usonia.foundation.Statuses
import usonia.server.client.BackendClient
import usonia.server.client.adjustForOffsets
import usonia.server.http.HttpRequest
import usonia.server.http.RestController
import usonia.server.http.RestResponse

/**
 * Publishes raw event data.
 */
internal class EventHttpPublisher(
    private val client: BackendClient,
    json: Json = Json,
    logger: KimchiLogger = EmptyLogger
): RestController<Event, Status>(json, logger) {
    override val method: String = "POST"
    override val path: String = "/events"
    override val deserializer = EventSerializer
    override val serializer = Status.serializer()

    override suspend fun getResponse(data: Event, request: HttpRequest): RestResponse<Status> {
        client.publishEvent(client.adjustForOffsets(data))
        return RestResponse(Statuses.SUCCESS)
    }
}
