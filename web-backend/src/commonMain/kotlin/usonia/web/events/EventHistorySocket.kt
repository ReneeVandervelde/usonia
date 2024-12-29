package usonia.web.events

import com.inkapplications.coroutines.ongoing.collectLatest
import kimchi.logger.EmptyLogger
import kimchi.logger.KimchiLogger
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import usonia.foundation.EventSerializer
import usonia.foundation.Identifier
import usonia.server.client.BackendClient
import usonia.server.http.WebSocketController

/**
 * Socket that outputs the latest event history for a particular device.
 */
internal class EventHistorySocket(
    private val client: BackendClient,
    private val json: Json = Json,
    private val logger: KimchiLogger = EmptyLogger,
): WebSocketController {
    override val path: String = "/events/history/{device}"

    override suspend fun start(input: ReceiveChannel<String>, output: SendChannel<String>, parameters: Map<String, List<String>>) {
        val deviceId = parameters["device"]
            ?.firstOrNull()
            ?.let(::Identifier)
            ?: run {
                logger.error("No device provided in events request")
                return
            }

        val count = parameters["count"]?.firstOrNull()?.toIntOrNull()

        client.deviceEventHistory(deviceId, count).collectLatest {
            output.send(json.encodeToString(ListSerializer(EventSerializer), it))
        }
    }
}
