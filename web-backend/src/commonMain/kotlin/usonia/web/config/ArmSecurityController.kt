package usonia.web.config

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import usonia.core.state.ConfigurationAccess
import usonia.foundation.Statuses
import usonia.server.http.HttpController
import usonia.server.http.HttpRequest
import usonia.server.http.HttpResponse

/**
 * Arms the site security system.
 */
class ArmSecurityController(
    private val config: ConfigurationAccess,
    private val json: Json,
): HttpController {
    override val path: String = "/security/arm"
    override val method: String = "PUT"

    override suspend fun requiresAuthorization(request: HttpRequest): Boolean = false

    override suspend fun getResponse(request: HttpRequest): HttpResponse {
        config.armSecurity()

        return HttpResponse(body = json.encodeToString(Statuses.SUCCESS))
    }
}
