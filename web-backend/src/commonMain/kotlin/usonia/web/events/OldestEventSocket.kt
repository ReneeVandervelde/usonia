package usonia.web.events

import com.inkapplications.coroutines.ongoing.collectLatest
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.json.Json
import usonia.foundation.InstantSerializer
import usonia.server.client.BackendClient
import usonia.server.http.WebSocketController

class OldestEventSocket(
    private val client: BackendClient,
    private val json: Json,
): WebSocketController {
    override val path: String = "/events/metric-oldest"

    override suspend fun start(input: ReceiveChannel<String>, output: SendChannel<String>, parameters: Map<String, List<String>>) {
        client.oldestEventTime.collectLatest {
            output.send(json.encodeToString(InstantSerializer.nullable, it))
        }
    }
}
