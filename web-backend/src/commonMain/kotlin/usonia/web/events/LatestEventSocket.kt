package usonia.web.events

import com.inkapplications.coroutines.ongoing.collectLatest
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import usonia.foundation.Identifier
import usonia.server.client.BackendClient
import usonia.server.http.WebSocketController

/**
 * Socket that outputs the latest event for a particular device.
 */
internal class LatestEventSocket(
    private val client: BackendClient,
    private val json: Json = Json,
    private val logger: KimchiLogger = EmptyLogger,
): WebSocketController {
    override val path: String = "/events/latest/{device}"

    override suspend fun start(
        input: ReceiveChannel<String>,
        output: SendChannel<String>,
        parameters: Map<String, List<String>>,
    ) {
        val deviceId = parameters["device"]
            ?.firstOrNull()
            ?.let(::Identifier)
            ?: run {
                logger.error("No device provided in events request")
                return
            }

        client.getLatestEvent(deviceId).collectLatest {
            output.send(json.encodeToString(it))
        }
    }
}
