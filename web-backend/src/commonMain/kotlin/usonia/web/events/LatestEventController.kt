package usonia.web.events

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import usonia.foundation.Event
import usonia.foundation.Identifier
import usonia.foundation.Statuses
import usonia.server.client.BackendClient
import usonia.server.http.HttpController
import usonia.server.http.HttpRequest
import usonia.server.http.HttpResponse

class LatestEventController(
    private val client: BackendClient,
    private val json: Json,
): HttpController {
    override val path: String = "/events/latest/{device}/{type}"

    override suspend fun getResponse(request: HttpRequest): HttpResponse {
        val deviceId = request.parameters["device"]
            ?.firstOrNull()
            ?.let(::Identifier)
            ?: return HttpResponse(
                body = Statuses.missingRequired("device").let(json::encodeToString),
                status = 400,
            )
        val type = request.parameters["type"]
            ?.firstOrNull()
            ?.let { type ->
                Event::class.sealedSubclasses.find { it.simpleName == type } ?: return HttpResponse(
                    body = Statuses.illegalArgument("type").let(json::encodeToString),
                    status = 400,
                )
            }
            ?: return HttpResponse(
                body = Statuses.missingRequired("type").let(json::encodeToString),
                status = 400,
            )
        val event = client.getState(deviceId, type) ?: return HttpResponse(
            body = Statuses.EVENT_NOT_FOUND.let(json::encodeToString),
            status = 404
        )

        return HttpResponse(
            body = json.encodeToString(event)
        )
    }
}
