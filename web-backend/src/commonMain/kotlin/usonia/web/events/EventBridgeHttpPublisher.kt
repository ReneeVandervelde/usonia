package usonia.web.events

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.serialization.json.Json
import usonia.core.server.HttpRequest
import usonia.core.server.RestController
import usonia.core.server.RestResponse
import usonia.core.state.ConfigurationAccess
import usonia.core.state.EventPublisher
import usonia.core.state.getSite
import usonia.foundation.*
import usonia.foundation.Statuses.DEVICE_NOT_FOUND
import usonia.foundation.Statuses.SUCCESS

private const val BRIDGE_PARAM = "bridge"

/**
 * Resolves devices from a bridge before publishing them as an Event.
 */
internal class EventBridgeHttpPublisher(
    private val eventPublisher: EventPublisher,
    private val configurationAccess: ConfigurationAccess,
    json: Json = Json,
    logger: KimchiLogger = EmptyLogger
): RestController<Event, Status>(json, logger) {
    override val method: String = "POST"
    override val path: String = "/bridges/{$BRIDGE_PARAM}/events"
    override val deserializer = EventSerializer
    override val serializer = Status.serializer()

    override suspend fun getResponse(data: Event, request: HttpRequest): RestResponse<Status> {
        val bridgeId = request.parameters[BRIDGE_PARAM]?.first() ?: return RestResponse(Statuses.missingRequired(BRIDGE_PARAM), status = 400)
        val deviceId = configurationAccess.getSite()
            .findDeviceBy { it.parent?.context?.value == bridgeId && it.parent?.id == data.source }
            ?.id
            ?: run {
                logger.error("Could not find device for event: <$data>")
                return RestResponse(DEVICE_NOT_FOUND)
            }

        eventPublisher.publishEvent(data.withSource(deviceId))
        return RestResponse(SUCCESS)
    }
}