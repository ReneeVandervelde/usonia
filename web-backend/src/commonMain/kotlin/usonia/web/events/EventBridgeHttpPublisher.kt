package usonia.web.events

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.serialization.json.Json
import usonia.core.state.getSite
import usonia.foundation.*
import usonia.foundation.Statuses.DEVICE_NOT_FOUND
import usonia.foundation.Statuses.SUCCESS
import usonia.server.client.BackendClient
import usonia.server.client.adjustForOffsets
import usonia.server.http.HttpRequest
import usonia.server.http.RestController
import usonia.server.http.RestResponse

private const val BRIDGE_PARAM = "bridge"

/**
 * Resolves devices from a bridge before publishing them as an Event.
 */
internal class EventBridgeHttpPublisher(
    private val client: BackendClient,
    json: Json = Json,
    logger: KimchiLogger = EmptyLogger
): RestController<Event, Status>(json, logger) {
    override val method: String = "POST"
    override val path: String = "/bridges/{$BRIDGE_PARAM}/events"
    override val deserializer = Event.serializer()
    override val serializer = Status.serializer()
    override suspend fun requiresAuthorization(data: Event, request: HttpRequest): Boolean {
        return data.isSensitive
    }

    override suspend fun getResponse(data: Event, request: HttpRequest): RestResponse<Status> {
        val bridgeId = request.parameters[BRIDGE_PARAM]?.first()?.let(::Identifier) ?: return RestResponse(Statuses.missingRequired(BRIDGE_PARAM), status = 400)
        val deviceId = client.getSite()
            .findBridgeDevice(bridgeId, data.source)
            ?.id
            ?: run {
                logger.error("Could not find device for event: <$data>")
                return RestResponse(DEVICE_NOT_FOUND)
            }

        client.publishEvent(data.withSource(deviceId).let { client.adjustForOffsets(it) })
        return RestResponse(SUCCESS)
    }
}
