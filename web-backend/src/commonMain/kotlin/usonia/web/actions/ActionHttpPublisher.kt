package usonia.web.actions

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.serialization.json.Json
import usonia.core.server.HttpRequest
import usonia.core.server.RestController
import usonia.core.server.RestResponse
import usonia.core.state.ActionPublisher
import usonia.foundation.Action
import usonia.foundation.ActionSerializer
import usonia.foundation.Status
import usonia.foundation.Statuses

/**
 * Publishes raw action data.
 */
internal class ActionHttpPublisher(
    private val actionPublisher: ActionPublisher,
    json: Json = Json,
    logger: KimchiLogger = EmptyLogger
): RestController<Action, Status>(json, logger) {
    override val deserializer = ActionSerializer
    override val serializer = Status.serializer()
    override val method: String = "POST"
    override val path: String = "/actions"

    override suspend fun getResponse(data: Action, request: HttpRequest): RestResponse<Status> {
        actionPublisher.publishAction(data)
        return RestResponse(Statuses.SUCCESS)
    }
}
