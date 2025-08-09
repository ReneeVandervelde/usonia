package usonia.web.actions

import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.serialization.json.Json
import usonia.foundation.Action
import usonia.foundation.Status
import usonia.foundation.Statuses
import usonia.server.client.BackendClient
import usonia.server.http.HttpRequest
import usonia.server.http.RestController
import usonia.server.http.RestResponse

/**
 * Publishes raw action data.
 */
internal class ActionHttpPublisher(
    private val client: BackendClient,
    json: Json = Json,
    logger: KimchiLogger = EmptyLogger
): RestController<Action, Status>(json, logger) {
    override val deserializer = Action.serializer()
    override val serializer = Status.serializer()
    override val method: String = "POST"
    override val path: String = "/actions"

    override suspend fun requiresAuthorization(data: Action, request: HttpRequest): Boolean {
        return when (data) {
            is Action.Intent -> when (data.action) {
                "usonia.rules.lights.WakeLight.dismiss" -> return false
                else -> super.requiresAuthorization(data, request)
            }
            else -> super.requiresAuthorization(data, request)
        }
    }

    override suspend fun getResponse(data: Action, request: HttpRequest): RestResponse<Status> {
        client.publishAction(data)
        return RestResponse(Statuses.SUCCESS)
    }
}
