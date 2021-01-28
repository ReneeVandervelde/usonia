package usonia.web.config

import kimchi.logger.KimchiLogger
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import usonia.core.state.ConfigurationAccess
import usonia.foundation.Status
import usonia.foundation.Statuses
import usonia.server.http.*

class FlagUpdateController(
    private val config: ConfigurationAccess,
    json: Json,
    logger: KimchiLogger,
): RestController<String?, Status>(json, logger) {
    override val path: String = "/flags/{key}"
    override val method: String = "PUT"
    override val serializer: KSerializer<Status> = Status.serializer()
    override val deserializer: KSerializer<String?> = String.serializer().nullable

    override suspend fun getResponse(data: String?, request: HttpRequest): RestResponse<Status> {
        val key = request.parameters["key"]?.firstOrNull() ?: return RestResponse(Statuses.missingRequired("key"))
        config.setFlag(key, data)
        return RestResponse(Statuses.SUCCESS)
    }
}
