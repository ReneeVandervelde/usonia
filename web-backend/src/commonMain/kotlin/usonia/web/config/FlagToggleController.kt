package usonia.web.config

import kimchi.logger.KimchiLogger
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import usonia.core.state.ConfigurationAccess
import usonia.core.state.toggleBooleanFlag
import usonia.foundation.Statuses
import usonia.server.http.HttpController
import usonia.server.http.HttpRequest
import usonia.server.http.HttpResponse

/**
 * Toggles a boolean flag from its current state.
 */
internal class FlagToggleController(
    private val config: ConfigurationAccess,
    private val json: Json,
    private val logger: KimchiLogger,
): HttpController {
    override val path: String = "/flags/{key}/toggle"
    override suspend fun getResponse(request: HttpRequest): HttpResponse {
        val key = request.parameters["key"]?.firstOrNull() ?: return HttpResponse(
            status = 400,
            body = json.encodeToString(Statuses.missingRequired("key"))
        )
        config.toggleBooleanFlag(key)
        return HttpResponse(body = json.encodeToString(Statuses.SUCCESS))
    }
}
