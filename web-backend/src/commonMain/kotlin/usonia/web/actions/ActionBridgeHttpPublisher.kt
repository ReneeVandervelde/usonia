package usonia.web.actions

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import usonia.core.server.HttpRequest
import usonia.core.server.RestController
import usonia.core.server.RestResponse
import usonia.core.state.ActionPublisher
import usonia.core.state.ConfigurationAccess
import usonia.core.state.getSite
import usonia.foundation.Action
import usonia.foundation.Status
import usonia.foundation.Statuses
import usonia.foundation.Statuses.SUCCESS
import usonia.foundation.findDeviceBy
import usonia.serialization.ActionSerializer
import usonia.serialization.StatusSerializer

private const val BRIDGE_PARAM = "bridge"

/**
 * Resolves devices from a bridge before publishing them as an Action.
 */
internal class ActionBridgeHttpPublisher(
    private val configurationAccess: ConfigurationAccess,
    private val actionPublisher: ActionPublisher,
    logger: KimchiLogger = EmptyLogger
): RestController<Action, Status>(logger) {
    override val path: String = "/bridges/{$BRIDGE_PARAM}/actions"
    override val method: String = "POST"
    override val deserializer = ActionSerializer
    override val serializer = StatusSerializer

    override suspend fun getResponse(data: Action, request: HttpRequest): RestResponse<Status> {
        val bridgeId = request.parameters[BRIDGE_PARAM]?.first() ?: return RestResponse(Statuses.missingRequired(BRIDGE_PARAM), status = 400)
        val deviceId = configurationAccess.getSite()
            .findDeviceBy { it.parent?.context?.value == bridgeId && it.parent?.id == data.target }
            ?.id
            ?: run {
                logger.error("Could not find device for action: <$data>")
                return RestResponse(Statuses.DEVICE_NOT_FOUND, status = 404)
            }

        actionPublisher.publishAction(data.withTarget(deviceId))
        return RestResponse(SUCCESS)
    }

}
