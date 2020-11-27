package usonia.bridge

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.serialization.KSerializer
import usonia.core.server.*
import usonia.foundation.Action
import usonia.serialization.ActionSerializer
import usonia.state.ActionPublisher

/**
 * Sends Actions to the action publisher.
 */
class ActionPublishHttpBridge(
    private val actionPublisher: ActionPublisher,
    logger: KimchiLogger = EmptyLogger
): RestController<Action, StatusResponse>(logger) {
    override val path: String = "/actions"
    override val method: String = "POST"
    override val deserializer: KSerializer<Action> = ActionSerializer
    override val serializer: KSerializer<StatusResponse> = StatusResponse.serializer()

    override suspend fun getResponse(data: Action, request: HttpRequest): RestResponse<StatusResponse> {
        actionPublisher.publishAction(data)
        return SUCCESS
    }

}
