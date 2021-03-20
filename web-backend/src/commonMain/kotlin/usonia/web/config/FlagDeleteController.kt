package usonia.web.config

import kotlinx.serialization.json.Json
import usonia.core.state.ConfigurationAccess
import usonia.foundation.Status
import usonia.foundation.Statuses
import usonia.server.http.HttpController
import usonia.server.http.HttpRequest
import usonia.server.http.HttpResponse

class FlagDeleteController(
    private val config: ConfigurationAccess,
    private val json: Json,
): HttpController {
    override val path: String = "/flags/{key}"
    override val method: String = "DELETE"

    override suspend fun getResponse(request: HttpRequest): HttpResponse {
        val key = request.parameters["key"]?.firstOrNull() ?: return HttpResponse(
            json.encodeToString(Status.serializer(), Statuses.missingRequired("key"))
        )

        config.removeFlag(key)

        return HttpResponse(
            body = json.encodeToString(Status.serializer(), Statuses.SUCCESS),
            contentType = "application/json"
        )
    }
}
