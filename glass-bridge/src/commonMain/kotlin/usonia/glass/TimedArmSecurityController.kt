package usonia.glass

import kimchi.logger.KimchiLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import usonia.core.state.ConfigurationAccess
import usonia.foundation.Status
import usonia.foundation.Statuses
import usonia.kotlin.DefaultScope
import usonia.server.http.HttpRequest
import usonia.server.http.RestController
import usonia.server.http.RestResponse
import kotlin.time.Duration.Companion.minutes

/**
 * Arms the security system after a delay to allow for exit.
 */
class TimedArmSecurityController(
    private val config: ConfigurationAccess,
    json: Json,
    logger: KimchiLogger,
    private val backgroundScope: CoroutineScope = DefaultScope(),
): RestController<Boolean, Status>(json, logger) {
    val delay = 10.minutes
    private val running = MutableStateFlow<Job?>(null)
    val isActive = running.map { it?.isActive ?: false }
    override val serializer: KSerializer<Status> = Status.serializer()
    override val deserializer: KSerializer<Boolean> = Boolean.serializer()
    override val path: String = "/glass/arm"
    override val method: String = "PUT"

    override suspend fun requiresAuthorization(data: Boolean, request: HttpRequest): Boolean {
        return false
    }

    override suspend fun getResponse(data: Boolean, request: HttpRequest): RestResponse<Status> {
        running.value?.cancel()
        running.value = null

        if (data) {
            running.value = backgroundScope.launch {
                delay(delay)
                config.armSecurity()
                running.value = null
            }
        }

        return RestResponse(Statuses.SUCCESS)
    }
}
