package usonia.web.actions

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import usonia.core.server.HttpRequest
import usonia.core.server.RestController
import usonia.core.server.RestResponse
import usonia.core.state.ActionPublisher
import usonia.foundation.Action
import usonia.foundation.Status
import usonia.foundation.Statuses
import usonia.serialization.ActionSerializer
import usonia.serialization.StatusSerializer

/**
 * Publishes raw action data.
 */
internal class ActionHttpPublisher(
    private val actionPublisher: ActionPublisher,
    logger: KimchiLogger = EmptyLogger
): RestController<Action, Status>(logger) {
    override val deserializer = ActionSerializer
    override val serializer = StatusSerializer
    override val path: String = "/actions"

    override suspend fun getResponse(data: Action, request: HttpRequest): RestResponse<Status> {
        actionPublisher.publishAction(data)
        return RestResponse(Statuses.SUCCESS)
    }
}
