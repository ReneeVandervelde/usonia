package usonia.web.config

import kimchi.logger.KimchiLogger
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import usonia.core.state.ConfigurationAccess
import usonia.foundation.Site
import usonia.foundation.Status
import usonia.foundation.Statuses
import usonia.server.http.HttpRequest
import usonia.server.http.RestController
import usonia.server.http.RestResponse

class SiteUpdateController(
    private val config: ConfigurationAccess,
    json: Json,
    logger: KimchiLogger,
): RestController<Site, Status>(json, logger) {
    override val path: String = "/site"
    override val method: String = "POST"
    override val serializer: KSerializer<Status> = Status.serializer()
    override val deserializer: KSerializer<Site> = Site.serializer()

    override suspend fun getResponse(data: Site, request: HttpRequest): RestResponse<Status> {
        config.updateSite(data)
        return RestResponse(Statuses.SUCCESS)
    }
}
